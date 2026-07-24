package com.lims.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 零部件 / 供应商 主数据查询。
 *
 * <p>对接外部主数据系统，dev profile 默认走 mock，生产由 application.yml 配置：
 * <pre>
 *   external.api.parts.base-url
 *   external.api.suppliers.base-url
 *   external.api.mock.enabled
 * </pre>
 */
@Slf4j
@Service
public class ExternalApiService {

    private final RestTemplate restTemplate;

    @Value("${external.api.parts.base-url:}")
    private String partsBaseUrl;

    @Value("${external.api.suppliers.base-url:}")
    private String suppliersBaseUrl;

    @Value("${external.api.mock.enabled:false}")
    private boolean mockEnabled;

    public ExternalApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "partService", fallbackMethod = "searchPartsFallback")
    @Cacheable(value = "partSearch", key = "#keyword", unless = "#result.isEmpty()")
    public List<Map<String, Object>> searchParts(String keyword) {
        if (mockEnabled || isBlank(partsBaseUrl)) {
            return mockParts(keyword);
        }
        String url = UriComponentsBuilder.fromHttpUrl(partsBaseUrl)
                .path("/api/parts").queryParam("keyword", keyword).toUriString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = restTemplate.getForObject(url, List.class);
        return result == null ? List.of() : result;
    }

    public List<Map<String, Object>> searchPartsFallback(String keyword, Throwable t) {
        log.warn("partsearch fallback: keyword={}, err={}", keyword, t.getMessage());
        return mockEnabled ? mockParts(keyword) : List.of();
    }

    @Cacheable(value = "partDetail", key = "#partNumber", unless = "#result == null")
    @CircuitBreaker(name = "partService", fallbackMethod = "getPartDetailFallback")
    public Map<String, Object> getPartDetail(String partNumber) {
        if (mockEnabled || isBlank(partsBaseUrl)) {
            return mockPartDetail(partNumber);
        }
        String url = UriComponentsBuilder.fromHttpUrl(partsBaseUrl)
                .path("/api/parts/" + partNumber).toUriString();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
        return result;
    }

    public Map<String, Object> getPartDetailFallback(String partNumber, Throwable t) {
        log.warn("partDetail fallback: partNumber={}, err={}", partNumber, t.getMessage());
        return mockEnabled ? mockPartDetail(partNumber) : null;
    }

    @CircuitBreaker(name = "supplierService", fallbackMethod = "searchSuppliersFallback")
    @Cacheable(value = "supplierSearch", key = "#keyword", unless = "#result.isEmpty()")
    public List<Map<String, Object>> searchSuppliers(String keyword) {
        if (mockEnabled || isBlank(suppliersBaseUrl)) {
            return mockSuppliers(keyword);
        }
        String url = UriComponentsBuilder.fromHttpUrl(suppliersBaseUrl)
                .path("/api/suppliers").queryParam("keyword", keyword).toUriString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = restTemplate.getForObject(url, List.class);
        return result == null ? List.of() : result;
    }

    public List<Map<String, Object>> searchSuppliersFallback(String keyword, Throwable t) {
        log.warn("supplierSearch fallback: keyword={}, err={}", keyword, t.getMessage());
        return mockEnabled ? mockSuppliers(keyword) : List.of();
    }

    @Cacheable(value = "supplierDetail", key = "#supplierCode", unless = "#result == null")
    @CircuitBreaker(name = "supplierService", fallbackMethod = "getSupplierDetailFallback")
    public Map<String, Object> getSupplierDetail(String supplierCode) {
        if (mockEnabled || isBlank(suppliersBaseUrl)) {
            return mockSupplierDetail(supplierCode);
        }
        String url = UriComponentsBuilder.fromHttpUrl(suppliersBaseUrl)
                .path("/api/suppliers/" + supplierCode).toUriString();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
        return result;
    }

    public Map<String, Object> getSupplierDetailFallback(String supplierCode, Throwable t) {
        log.warn("supplierDetail fallback: supplierCode={}, err={}", supplierCode, t.getMessage());
        return mockEnabled ? mockSupplierDetail(supplierCode) : null;
    }

    /* --------- mock --------- */

    private List<Map<String, Object>> mockParts(String keyword) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) return list;
        for (int i = 1; i <= 3; i++) {
            list.add(Map.of(
                    "partNumber", keyword.toUpperCase() + "-MOCK-" + String.format("%03d", i),
                    "partName", "Mock part " + i + " for " + keyword,
                    "eco", "ECO-2026-" + String.format("%04d", i),
                    "specification", "spec-" + i,
                    "source", "mock"
            ));
        }
        return list;
    }

    private Map<String, Object> mockPartDetail(String partNumber) {
        return Map.of(
                "partNumber", partNumber,
                "partName", "Mock part " + partNumber,
                "category", "MockCategory",
                "eco", "ECO-2026-MOCK",
                "supplier", "MockSupplier",
                "source", "mock"
        );
    }

    private List<Map<String, Object>> mockSuppliers(String keyword) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) return list;
        for (int i = 1; i <= 3; i++) {
            list.add(Map.of(
                    "supplierCode", "SUP-" + keyword.toUpperCase() + "-" + String.format("%03d", i),
                    "supplierName", "Mock " + keyword + " Supplier " + i,
                    "country", "CN",
                    "level", "A",
                    "source", "mock"
            ));
        }
        return list;
    }

    private Map<String, Object> mockSupplierDetail(String supplierCode) {
        return Map.of(
                "supplierCode", supplierCode,
                "supplierName", "Mock " + supplierCode,
                "country", "CN",
                "contact", "mock@example.com",
                "level", "A",
                "source", "mock"
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
