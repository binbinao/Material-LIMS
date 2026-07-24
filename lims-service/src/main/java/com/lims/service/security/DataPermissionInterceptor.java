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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import java.sql.SQLException;

/**
 * 数据权限拦截器：根据当前用户角色，对涉及 request / report / analysis_task / sample
 * 等核心业务表的查询，自动追加 WHERE 条件，确保用户只能看到与自己相关的数据行。
 *
 * 权限映射：
 *   request       → requester_id = 当前用户（委托发起人只能看自己的委托）
 *   analysis_task → assignee_id  = 当前用户（工程师只能看分配给自己的任务）
 *   report        → author_id    = 当前用户（作者只能看自己写的报告）
 *   sample        → received_by  = 当前用户（技术员只能看自己接收的样品）
 *
 * 注意：基础数据表（brand / department / equipment 等）和 sys_user 不参与过滤。
 *      MANAGER / ADMIN 也不过滤（看全量）。
 *
 * 实现策略：仅当 SQL 是 SELECT 且 from 单表为受控表 + 当前用户为非管理者角色时，注入条件。
 * 复杂查询优先通过正则回退机制处理；无法证明受控表已过滤时拒绝执行，避免
 * 在 SQL 解析失败时把业务数据暴露给普通用户。
 */
@Slf4j
@Component
public class DataPermissionInterceptor implements InnerInterceptor {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final Set<String> KNOWN_CONTROLLED_TABLES =
            Set.of("request", "analysis_task", "report", "sample");
    private static final Pattern CONTROLLED_TABLE_REFERENCE = Pattern.compile(
            "(?i)\\b(?:from|join)\\s+(?:[\\w\\\"`]+\\.)?[\\\"`]?"
                    + "(request|analysis_task|report|sample)\\b");
    private static final AtomicLong FILTER_FAILURES = new AtomicLong();

    /**
     * True when the active Spring profile contains "dev". In dev we
     * skip row-level permission injection entirely — DevAuthFilter
     * still synthesizes the real role set per X-Dev-User header
     * (engineer gets ENGINEER only, manager gets MANAGER only, etc.)
     * so that the controller-level @PreAuthorize and the four-eyes
     * service guard fire correctly. Without this, dev users would
     * see "row not found" instead of the real 403/3002 from the
     * authorization layer.
     */
    private final boolean devProfile;

    public DataPermissionInterceptor(Environment env) {
        this.devProfile = Arrays.asList(env.getActiveProfiles()).contains("dev");
        if (devProfile) {
            log.warn("DataPermissionInterceptor running in DISABLED mode (dev profile).");
        }
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
            throws SQLException {
        if (devProfile) {
            return;
        }
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
            // jsqlparser 4.9: PlainSelect 实现了 Select 接口；复杂查询（SetOperationList 等）
            // 必须先确认不涉及受控表，不能静默放行。
            if (!(stmt instanceof PlainSelect ps)) {
                if (containsControlledTableReference(originalSql)) {
                    failClosed("unsupported select shape");
                }
                return;
            }
            if (!(ps.getFromItem() instanceof Table table)) {
                if (containsControlledTableReference(originalSql)) {
                    failClosed("unsupported FROM item");
                }
                return;
            }
            String tableName = stripQuotes(table.getName()).toLowerCase();

            Expression injected = buildPermissionPredicate(tableName, principal);
            if (injected == null) return;

            Expression where = ps.getWhere();
            ps.setWhere(where == null ? injected : new AndExpression(where, injected));
            String newSql = ps.toString();
            PluginUtils.mpBoundSql(boundSql).sql(newSql);
            if (log.isDebugEnabled()) {
                log.debug("Data permission injected. table={}, user={}", tableName, principal.userId());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[DataPermission] SQL parse failed for user={}; attempting controlled fallback. error={}",
                    principal.userId(), e.getMessage());
            String outerTable = tryRegexFallback(originalSql);
            if (outerTable != null) {
                Expression injected = buildPermissionPredicate(outerTable, principal);
                if (injected != null) {
                    String newSql = injectWhere(originalSql, injected, outerTable);
                    if (newSql != null) {
                        PluginUtils.mpBoundSql(boundSql).sql(newSql);
                        log.info("[DataPermission] Regex fallback injected permission predicate. table={}, user={}",
                                outerTable, principal.userId());
                        return;
                    }
                }
            }
            if (containsControlledTableReference(originalSql)) {
                failClosed("unable to parse controlled-table query", e);
            }
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
            case "sample":
                return eq("received_by", userId);
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

    /**
     * Regex-based fallback for queries jsqlparser can't parse
     * (JOIN/UNION/CTE). Extracts the first table name after FROM, handling
     * aliases: for {@code FROM request r}, returns {@code request} not
     * {@code r}. Only returns a name that matches a known controlled table
     * (request / analysis_task / report / sample); otherwise returns null.
     *
     * Strategy: capture all consecutive identifier tokens after FROM, then
     * pick the first one that matches a controlled table name (case-insensitive).
     * This handles: FROM request, FROM request r, FROM request AS r, FROM schema.request.
     */
    static String tryRegexFallback(String sql) {
        if (sql == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?i)\\bfrom\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\s+(?:as\\s+)?[A-Za-z_][A-Za-z0-9_]*)*)")
                .matcher(sql);
        if (!m.find()) return null;
        // Split the captured group into tokens, clean quotes, check against known tables
        String[] tokens = m.group(1).split("\\s+");
        for (String token : tokens) {
            String cleaned = stripQuotes(token).toLowerCase();
            if (KNOWN_CONTROLLED_TABLES.contains(cleaned)) {
                return cleaned;
            }
            // Skip alias keywords like "AS"
        }
        return null;
    }

    static boolean containsControlledTableReference(String sql) {
        return sql != null && CONTROLLED_TABLE_REFERENCE.matcher(sql).find();
    }

    static long filterFailureCount() {
        return FILTER_FAILURES.get();
    }

    private static void failClosed(String reason) {
        long failureCount = FILTER_FAILURES.incrementAndGet();
        log.error("[DataPermission] refusing unverified controlled-table query. reason={}, failures={}",
                reason, failureCount);
        throw new BusinessException(ErrorCode.DATA_PERMISSION_FILTER_FAILED, reason);
    }

    private static void failClosed(String reason, Exception cause) {
        long failureCount = FILTER_FAILURES.incrementAndGet();
        log.error("[DataPermission] refusing unverified controlled-table query. reason={}, failures={}, error={}",
                reason, failureCount, cause.getMessage());
        throw new BusinessException(ErrorCode.DATA_PERMISSION_FILTER_FAILED, reason);
    }

    /**
     * Naive WHERE injector: if the SQL has no {@code WHERE} clause,
     * append {@code WHERE <predicate>}; else append {@code AND <predicate>}.
     * Returns null if the table name can't be parsed as a known
     * controlled table.
     */
    private static String injectWhere(String sql, Expression injected, String outerTable) {
        String predicate = injected.toString();
        java.util.regex.Matcher whereM = java.util.regex.Pattern.compile(
                "(?i)\\bwhere\\b").matcher(sql);
        String newSql;
        if (whereM.find()) {
            int idx = whereM.end();
            newSql = sql.substring(0, idx) + " (" + predicate + ") AND" + sql.substring(idx);
        } else {
            // No WHERE — append before any GROUP BY / ORDER BY / LIMIT / HAVING.
            java.util.regex.Matcher endM = java.util.regex.Pattern.compile(
                    "(?i)\\b(group\\s+by|order\\s+by|having|limit)\\b").matcher(sql);
            if (endM.find()) {
                int idx = endM.start();
                newSql = sql.substring(0, idx) + " WHERE (" + predicate + ") " + sql.substring(idx);
            } else {
                newSql = sql + " WHERE (" + predicate + ")";
            }
        }
        return newSql;
    }
}
