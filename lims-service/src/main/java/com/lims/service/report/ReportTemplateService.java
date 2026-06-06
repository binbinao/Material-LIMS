package com.lims.service.report;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.dao.mapper.AnalysisItemMapper;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.BrandMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.AnalysisItem;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Brand;
import com.lims.model.entity.Request;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 基于 poi-tl 渲染报告 docx 模板。
 * 模板位置：classpath:templates/report_template.docx（若不存在则用内置最小模板）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    private static final String TEMPLATE_RESOURCE = "templates/report_template.docx";

    private final RequestMapper requestMapper;
    private final BrandMapper brandMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisItemMapper analysisItemMapper;

    /**
     * 生成报告 docx，返回临时文件路径。
     */
    public Path generate(String requestId, String reportNo, String versionNumber, String revisionNote) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "Request not found: " + requestId);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("reportNo", reportNo == null ? "" : reportNo);
        data.put("requestNo", nullToEmpty(request.getRequestNo()));
        data.put("requestDate", request.getCreatedAt() == null
                ? "" : request.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        data.put("dueDate", request.getDueDate() == null
                ? "" : request.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        data.put("brand", brandName(request.getBrandId()));
        data.put("partNumber", nullToEmpty(request.getPartNumber()));
        data.put("partName", nullToEmpty(request.getPartName()));
        data.put("eco", nullToEmpty(request.getEco()));
        data.put("supplierName", nullToEmpty(request.getSupplierName()));
        data.put("supplierCode", nullToEmpty(request.getSupplierCode()));
        data.put("requestReason", nullToEmpty(request.getRequestReason()));
        data.put("priority", nullToEmpty(request.getPriority()));
        data.put("versionNumber", nullToEmpty(versionNumber));
        data.put("revisionNote", nullToEmpty(revisionNote));
        data.put("isRevision", revisionNote != null && !revisionNote.isBlank());
        data.put("generatedAt", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        // analysis tasks
        List<AnalysisTask> tasks = analysisTaskMapper.selectList(
                new LambdaQueryWrapper<AnalysisTask>().eq(AnalysisTask::getRequestId, requestId));
        List<Map<String, Object>> taskRows = new ArrayList<>();
        for (AnalysisTask t : tasks) {
            AnalysisItem item = analysisItemMapper.selectById(t.getItemId());
            Map<String, Object> row = new HashMap<>();
            row.put("analysisItem", item == null ? "" : nullToEmpty(item.getName()));
            row.put("testStandards", item == null ? "" : nullToEmpty(item.getTestStandards()));
            row.put("status", nullToEmpty(t.getStatus()));
            row.put("result", "");
            taskRows.add(row);
        }
        data.put("analysisTasks", taskRows);

        Path output = Paths.get(System.getProperty("java.io.tmpdir"),
                "lims-reports", "report_" + UUID.randomUUID() + ".docx");
        try {
            Files.createDirectories(output.getParent());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to prepare output dir: " + e.getMessage());
        }

        ClassPathResource template = new ClassPathResource(TEMPLATE_RESOURCE);
        if (template.exists()) {
            try (InputStream tplIn = template.getInputStream();
                 FileOutputStream out = new FileOutputStream(output.toFile())) {
                XWPFTemplate compiled = XWPFTemplate.compile(tplIn, Configure.createDefault()).render(data);
                compiled.write(out);
                compiled.close();
                log.info("Generated report docx via poi-tl: {}", output);
                return output;
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Template render failed: " + e.getMessage());
            }
        }

        // No classpath template found — write a minimal plain-text-as-docx placeholder
        try {
            String body = buildPlainBody(data);
            Files.writeString(output, body);
            log.warn("templates/report_template.docx not found; wrote text placeholder at {}", output);
            return output;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Placeholder write failed: " + e.getMessage());
        }
    }

    private String brandName(String brandId) {
        if (brandId == null) return "";
        Brand b = brandMapper.selectById(brandId);
        return b == null ? "" : nullToEmpty(b.getName());
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private String buildPlainBody(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("LIMS Material Test Report (placeholder)\n");
        sb.append("---------------------------------------\n");
        for (var entry : data.entrySet()) {
            if (!"analysisTasks".equals(entry.getKey())) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
        }
        sb.append("\n--- Analysis Tasks ---\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.getOrDefault("analysisTasks", List.of());
        for (Map<String, Object> r : rows) {
            sb.append("- ").append(r.get("analysisItem")).append(" [").append(r.get("status")).append("]\n");
        }
        return sb.toString();
    }
}
