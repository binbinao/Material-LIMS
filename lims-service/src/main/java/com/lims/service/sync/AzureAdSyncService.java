package com.lims.service.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.dao.mapper.DepartmentMapper;
import com.lims.dao.mapper.SysUserMapper;
import com.lims.model.entity.Department;
import com.lims.model.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Azure AD / Microsoft Graph user & department sync service.
 * 在 azure.ad.enabled=false 时（dev profile 默认），MicrosoftGraphClient 不存在，跳过同步。
 */
@Slf4j
@Service
public class AzureAdSyncService {

    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final ObjectProvider<MicrosoftGraphClient> graphClientProvider;

    public AzureAdSyncService(SysUserMapper sysUserMapper,
                              DepartmentMapper departmentMapper,
                              ObjectProvider<MicrosoftGraphClient> graphClientProvider) {
        this.sysUserMapper = sysUserMapper;
        this.departmentMapper = departmentMapper;
        this.graphClientProvider = graphClientProvider;
    }

    private MicrosoftGraphClient graphClient() {
        return graphClientProvider.getIfAvailable();
    }

    /**
     * 整点执行：同步用户。
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void syncUsers() {
        MicrosoftGraphClient client = graphClient();
        if (client == null) {
            log.debug("Azure AD sync skipped (azure.ad.enabled=false)");
            return;
        }
        log.info("Starting Azure AD user sync...");
        List<Map<String, Object>> graphUsers;
        try {
            graphUsers = client.listUsers();
        } catch (Exception e) {
            log.error("Azure AD user sync failed during Graph call", e);
            return;
        }

        int created = 0, updated = 0;
        Set<String> seenExternalIds = new HashSet<>();

        for (Map<String, Object> gu : graphUsers) {
            String externalId = asString(gu.get("id"));
            String email = firstNonBlank(asString(gu.get("mail")), asString(gu.get("userPrincipalName")));
            String displayName = firstNonBlank(asString(gu.get("displayName")), email);
            String upn = asString(gu.get("userPrincipalName"));
            if (externalId == null || email == null) continue;
            seenExternalIds.add(externalId);

            SysUser existing = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getExternalId, externalId));
            if (existing == null) {
                // fallback: match by email
                existing = sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
            }
            if (existing != null) {
                boolean changed = false;
                if (!Objects.equals(existing.getDisplayName(), displayName)) { existing.setDisplayName(displayName); changed = true; }
                if (!Objects.equals(existing.getEmail(), email)) { existing.setEmail(email); changed = true; }
                if (!Objects.equals(existing.getLoginId(), upn)) { existing.setLoginId(upn); changed = true; }
                if (!Objects.equals(existing.getExternalId(), externalId)) { existing.setExternalId(externalId); changed = true; }
                if (Boolean.FALSE.equals(existing.getIsActive())) { existing.setIsActive(true); changed = true; }
                if (changed) {
                    sysUserMapper.updateById(existing);
                    updated++;
                }
            } else {
                SysUser u = new SysUser();
                u.setEmail(email);
                u.setDisplayName(displayName);
                u.setLoginId(upn);
                u.setExternalId(externalId);
                u.setRoles("REQUESTER");
                u.setIsActive(true);
                sysUserMapper.insert(u);
                created++;
            }
        }

        // mark users not seen in graph as inactive
        int deactivated = 0;
        List<SysUser> active = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getIsActive, true).isNotNull(SysUser::getExternalId));
        for (SysUser u : active) {
            if (u.getExternalId() != null && !seenExternalIds.contains(u.getExternalId())) {
                u.setIsActive(false);
                sysUserMapper.updateById(u);
                deactivated++;
            }
        }
        log.info("Azure AD user sync done. created={}, updated={}, deactivated={}, total={}",
                created, updated, deactivated, graphUsers.size());
    }

    /**
     * 半小时执行：同步部门（来自 Graph Groups）。
     */
    @Scheduled(cron = "0 30 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void syncDepartments() {
        MicrosoftGraphClient client = graphClient();
        if (client == null) {
            log.debug("Azure AD department sync skipped (azure.ad.enabled=false)");
            return;
        }
        log.info("Starting Azure AD department sync...");
        List<Map<String, Object>> groups;
        try {
            groups = client.listGroups();
        } catch (Exception e) {
            log.error("Azure AD department sync failed during Graph call", e);
            return;
        }

        int created = 0, updated = 0;
        for (Map<String, Object> g : groups) {
            String externalId = asString(g.get("id"));
            String name = asString(g.get("displayName"));
            if (externalId == null || name == null) continue;

            Department existing = departmentMapper.selectOne(
                    new LambdaQueryWrapper<Department>().eq(Department::getExternalId, externalId));
            if (existing != null) {
                if (!Objects.equals(existing.getName(), name)) {
                    existing.setName(name);
                    departmentMapper.updateById(existing);
                    updated++;
                }
            } else {
                Department d = new Department();
                d.setName(name);
                d.setExternalId(externalId);
                d.setLevel(1);
                d.setSortOrder(0);
                departmentMapper.insert(d);
                created++;
            }
        }
        log.info("Azure AD department sync done. created={}, updated={}, total={}", created, updated, groups.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualSyncUsers() {
        log.info("Manual Azure AD user sync triggered");
        syncUsers();
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualSyncDepartments() {
        log.info("Manual Azure AD department sync triggered");
        syncDepartments();
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
