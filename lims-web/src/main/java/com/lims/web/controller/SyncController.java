package com.lims.web.controller;

import com.lims.common.result.R;
import com.lims.service.sync.AzureAdSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Data Sync", description = "数据同步")
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final AzureAdSyncService azureAdSyncService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> syncUsers() {
        azureAdSyncService.manualSyncUsers();
        return R.ok();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> syncDepartments() {
        azureAdSyncService.manualSyncDepartments();
        return R.ok();
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> getStatus() {
        return R.ok("Sync service running. Next scheduled sync at the top of the hour.");
    }
}
