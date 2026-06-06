package com.lims.web.controller;

import com.lims.common.result.R;
import com.lims.service.ExternalApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "External Integration", description = "外部系统集成")
@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    @Operation(summary = "Search parts from master data system")
    @GetMapping("/parts")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, Object>>> searchParts(@RequestParam String keyword) {
        return R.ok(externalApiService.searchParts(keyword));
    }

    @Operation(summary = "Get part detail by partNumber")
    @GetMapping("/parts/{partNumber}")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getPartDetail(@PathVariable String partNumber) {
        return R.ok(externalApiService.getPartDetail(partNumber));
    }

    @Operation(summary = "Search suppliers from supplier management system")
    @GetMapping("/suppliers")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, Object>>> searchSuppliers(@RequestParam String keyword) {
        return R.ok(externalApiService.searchSuppliers(keyword));
    }

    @Operation(summary = "Get supplier detail by supplierCode")
    @GetMapping("/suppliers/{supplierCode}")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getSupplierDetail(@PathVariable String supplierCode) {
        return R.ok(externalApiService.getSupplierDetail(supplierCode));
    }
}
