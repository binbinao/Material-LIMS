package com.lims.service.security;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.JwtTokenProvider;
import com.lims.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * 数据权限拦截器：根据当前用户角色，对涉及 request / report / analysis_task 等核心业务表的查询，
 * 自动追加 WHERE 条件。
 *
 * 注意：基础数据表（brand / department / equipment 等）和 sys_user 不参与过滤。
 *      MANAGER / ADMIN 也不过滤（看全量）。
 *
 * 实现策略：仅当 SQL 是 SELECT 且 from 单表为受控表 + 当前用户为非管理者角色时，注入条件。
 * 复杂 join 查询不在 MVP 拦截范围内（会被忽略并打日志）。
 */
@Slf4j
@Component
public class DataPermissionInterceptor implements InnerInterceptor {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
            throws SQLException {
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
            return;
        }
        JwtTokenProvider.AuthPrincipal principal = SecurityUtils.getCurrentPrincipal();
        if (principal == null) return;
        if (principal.hasRole(ROLE_ADMIN) || principal.hasRole(ROLE_MANAGER)) return;

        String originalSql = boundSql.getSql();
        try {
            Statement stmt = CCJSqlParserUtil.parse(originalSql);
            if (!(stmt instanceof Select)) return;
            // jsqlparser 4.9: PlainSelect 实现了 Select 接口；复杂查询（SetOperationList 等）跳过
            if (!(stmt instanceof PlainSelect ps)) return;
            if (!(ps.getFromItem() instanceof Table table)) return;
            String tableName = stripQuotes(table.getName()).toLowerCase();

            Expression injected = buildPermissionPredicate(tableName, principal);
            if (injected == null) return;

            Expression where = ps.getWhere();
            ps.setWhere(where == null ? injected : new AndExpression(where, injected));
            String newSql = ps.toString();
            PluginUtils.mpBoundSql(boundSql).sql(newSql);
            if (log.isDebugEnabled()) {
                log.debug("Data permission injected. table={}, user={}, sql={}", tableName, principal.userId(), newSql);
            }
        } catch (Exception e) {
            // Issue #6: fail closed. Non-ADMIN/MANAGER users have already
            // entered the row-level-filter path; if jsqlparser can't parse
            // the SQL, falling through with the original would silently
            // return unfiltered rows (any JOIN/UNION/subquery would bypass
            // the filter). Throw so the query fails loudly instead of
            // exfiltrating data. ADMIN/MANAGER have early-returned above
            // and never reach this catch.
            log.error("DataPermission parse failed, failing closed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DATA_PERMISSION_FILTER_FAILED,
                    "Row-level permission filter could not be applied to this query. " +
                            "Refusing to run unfiltered. (" + e.getMessage() + ")");
        }
    }

    private Expression buildPermissionPredicate(String table, JwtTokenProvider.AuthPrincipal p) {
        String userId = p.userId();
        if (userId == null) return null;

        switch (table) {
            case "request":
                return eq("requester_id", userId);
            case "analysis_task":
                return eq("assignee_id", userId);
            case "report":
                return eq("author_id", userId);
            default:
                return null;
        }
    }

    private static EqualsTo eq(String column, String value) {
        EqualsTo eq = new EqualsTo();
        eq.setLeftExpression(new Column(column));
        eq.setRightExpression(new StringValue(value));
        return eq;
    }

    private static String stripQuotes(String s) {
        if (s == null) return "";
        return s.replace("\"", "").replace("`", "");
    }
}
