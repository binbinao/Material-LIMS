package com.lims.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.result.R;
import com.lims.dao.mapper.AuditLogMapper;
import com.lims.dao.mapper.SysUserMapper;
import com.lims.model.entity.AuditLog;
import com.lims.model.entity.SysUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Admin", description = "系统管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;

    @Operation(summary = "List users with pagination")
    @GetMapping("/users")
    public R<Page<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null) {
            wrapper.and(w -> w.like(SysUser::getDisplayName, keyword)
                    .or().like(SysUser::getEmail, keyword)
                    .or().like(SysUser::getLoginId, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        long current = page <= 0 ? 1 : page;
        return R.ok(sysUserMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @Operation(summary = "Update user roles")
    @PutMapping("/users/{id}/roles")
    public R<Void> updateUserRoles(@PathVariable String id, @RequestBody Map<String, String> body) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return R.fail("User not found");
        user.setRoles(body.get("roles"));
        sysUserMapper.updateById(user);
        return R.ok();
    }

    @Operation(summary = "Toggle user active status")
    @PutMapping("/users/{id}/toggle-active")
    public R<Void> toggleUserActive(@PathVariable String id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return R.fail("User not found");
        user.setIsActive(user.getIsActive() == null || !user.getIsActive());
        sysUserMapper.updateById(user);
        return R.ok();
    }

    /**
     * 列出审计日志，支持 module / action / userId / 日期区间过滤；返回行附带 userName 字段（前端不用单独查）。
     */
    @Operation(summary = "List audit logs with filters and user-name join")
    @GetMapping("/logs")
    public R<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) wrapper.eq(AuditLog::getModule, module);
        if (action != null && !action.isBlank()) wrapper.eq(AuditLog::getAction, action);
        if (userId != null && !userId.isBlank()) wrapper.eq(AuditLog::getUserId, userId);
        if (startDate != null) wrapper.ge(AuditLog::getCreatedAt, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(AuditLog::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        long current = page <= 0 ? 1 : page;
        Page<AuditLog> p = auditLogMapper.selectPage(new Page<>(current, size), wrapper);

        // batch resolve userId -> displayName
        Set<String> ids = p.getRecords().stream()
                .map(AuditLog::getUserId)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> userMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));
            for (SysUser u : users) {
                userMap.put(u.getId(),
                        u.getDisplayName() != null ? u.getDisplayName() : u.getLoginId());
            }
        }

        List<Map<String, Object>> rows = p.getRecords().stream().map(log -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("userId", log.getUserId());
            row.put("userName", userMap.getOrDefault(log.getUserId(), log.getUserId()));
            row.put("module", log.getModule());
            row.put("action", log.getAction());
            row.put("entityId", log.getEntityId());
            row.put("detail", log.getDetail());
            row.put("ip", log.getIp());
            row.put("createdAt", log.getCreatedAt());
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", p.getTotal());
        result.put("size", p.getSize());
        result.put("current", p.getCurrent());
        return R.ok(result);
    }

    @Operation(summary = "Get audit log detail by id")
    @GetMapping("/logs/{id}")
    public R<AuditLog> getLog(@PathVariable String id) {
        return R.ok(auditLogMapper.selectById(id));
    }
}
