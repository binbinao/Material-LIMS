package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.Holiday;
import com.lims.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Holiday Management", description = "节假日管理 / 工作日计算")
@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<Holiday>> list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) Integer year) {
        return R.ok(holidayService.list(page, size, year));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Holiday> getById(@PathVariable String id) {
        return R.ok(holidayService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "HOLIDAY", action = "CREATE")
    public R<Holiday> create(@Valid @RequestBody Holiday entity) {
        return R.ok(holidayService.create(entity));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "HOLIDAY", action = "IMPORT")
    public R<Void> batchImport(@RequestBody List<@Valid Holiday> holidays) {
        holidayService.batchImport(holidays);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "HOLIDAY", action = "UPDATE")
    public R<Holiday> update(@PathVariable String id, @Valid @RequestBody Holiday entity) {
        return R.ok(holidayService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "HOLIDAY", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        holidayService.delete(id);
        return R.ok();
    }

    /* --------- 工作日计算 --------- */

    @Operation(summary = "Compute due date by skipping holidays/weekends")
    @GetMapping("/calculate-due-date")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> calcDueDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam int days) {
        LocalDate due = holidayService.addBusinessDays(baseDate, days);
        return R.ok(Map.of("baseDate", baseDate, "days", days, "dueDate", due));
    }

    @Operation(summary = "Check whether the given date is a business day")
    @GetMapping("/is-business-day")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> isBusinessDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(Map.of("date", date, "isBusinessDay", holidayService.isBusinessDay(date)));
    }

    @Operation(summary = "Count business days between [from, to] (inclusive)")
    @GetMapping("/count-business-days")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> countBusinessDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        int count = holidayService.countBusinessDays(from, to);
        return R.ok(Map.of("from", from, "to", to, "count", count));
    }
}
