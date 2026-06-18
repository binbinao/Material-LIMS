package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.KnowledgeDoc;
import com.lims.service.KnowledgeDocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Knowledge Hub", description = "知识库文档")
@RestController
@RequestMapping("/api/v1/knowledge-docs")
@RequiredArgsConstructor
public class KnowledgeDocController {

    private final KnowledgeDocService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<KnowledgeDoc>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(service.list(page, size, category, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<KnowledgeDoc> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "Upload knowledge document (multipart)")
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @AuditLog(module = "KNOWLEDGE", action = "UPLOAD")
    public R<KnowledgeDoc> upload(@RequestPart("file") MultipartFile file,
                                  @RequestParam(required = false) String title,
                                  @RequestParam String category,
                                  @RequestParam(required = false) String description) {
        return R.ok(service.upload(file, title, category, description));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @AuditLog(module = "KNOWLEDGE", action = "UPDATE")
    public R<KnowledgeDoc> updateMeta(@PathVariable String id, @RequestBody Map<String, String> body) {
        return R.ok(service.updateMeta(id, body.get("title"), body.get("description")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "KNOWLEDGE", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }
}
