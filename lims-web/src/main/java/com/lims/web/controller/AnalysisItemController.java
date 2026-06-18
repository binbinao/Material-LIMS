package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.AnalysisItem;
import com.lims.model.vo.AnalysisItemCascadeVO;
import com.lims.service.AnalysisItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Analysis Item Management", description = "分析项目管理")
@RestController
@RequestMapping("/api/v1/analysis-items")
@RequiredArgsConstructor
public class AnalysisItemController {

    private final AnalysisItemService analysisItemService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<AnalysisItem>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String groupId,
                                       @RequestParam(required = false) String typeId) {
        return R.ok(analysisItemService.list(page, size, groupId, typeId));
    }

    @GetMapping("/by-group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List items by test group")
    public R<List<AnalysisItem>> listByGroup(@PathVariable String groupId) {
        return R.ok(analysisItemService.listByGroup(groupId));
    }

    @GetMapping("/cascade")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get cascade data for frontend selection")
    public R<List<AnalysisItemCascadeVO>> cascade() {
        return R.ok(analysisItemService.cascade());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<AnalysisItem> getById(@PathVariable String id) {
        return R.ok(analysisItemService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "ANALYSIS_ITEM", action = "CREATE")
    public R<AnalysisItem> create(@Valid @RequestBody AnalysisItem entity) {
        return R.ok(analysisItemService.create(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "ANALYSIS_ITEM", action = "UPDATE")
    public R<AnalysisItem> update(@PathVariable String id, @Valid @RequestBody AnalysisItem entity) {
        return R.ok(analysisItemService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "ANALYSIS_ITEM", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        analysisItemService.delete(id);
        return R.ok();
    }
}
