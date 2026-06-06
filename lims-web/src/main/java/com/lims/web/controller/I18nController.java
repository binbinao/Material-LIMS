package com.lims.web.controller;

import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.service.I18nService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "i18n", description = "国际化字典")
@RestController
@RequestMapping("/api/v1/i18n")
@RequiredArgsConstructor
public class I18nController {

    private final I18nService i18nService;

    @GetMapping("/messages")
    public R<Map<String, String>> getMessages(@RequestParam(defaultValue = "zh-CN") String locale) {
        return R.ok(i18nService.getMessages(locale));
    }

    @PostMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "I18N", action = "UPSERT")
    public R<Void> upsert(@RequestBody Map<String, Object> body) {
        String key = (String) body.get("messageKey");
        String locale = (String) body.get("locale");
        String value = (String) body.get("messageValue");
        i18nService.upsert(key, locale, value);
        return R.ok();
    }

    @PostMapping("/messages/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "I18N", action = "BATCH_UPSERT")
    public R<Void> batchUpsert(@RequestParam String locale, @RequestBody Map<String, String> messages) {
        i18nService.batchUpsert(locale, messages);
        return R.ok();
    }

    @DeleteMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "I18N", action = "DELETE")
    public R<Void> delete(@RequestParam String messageKey, @RequestParam String locale) {
        i18nService.delete(messageKey, locale);
        return R.ok();
    }
}
