package com.lims.service.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.dao.mapper.AnalysisItemMapper;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.BrandMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.dao.mapper.RequestTypeMapper;
import com.lims.model.entity.AnalysisItem;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Brand;
import com.lims.model.entity.Request;
import com.lims.model.entity.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成本统计：支持按 brand / type / month / item 维度聚合，以及 Excel 导出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostStatsService {

    public static final String GROUP_BY_BRAND = "brand";
    public static final String GROUP_BY_TYPE = "type";
    public static final String GROUP_BY_MONTH = "month";
    public static final String GROUP_BY_ITEM = "item";

    private final RequestMapper requestMapper;
    private final AnalysisTaskMapper taskMapper;
    private final AnalysisItemMapper itemMapper;
    private final BrandMapper brandMapper;
    private final RequestTypeMapper requestTypeMapper;

    /**
     * 维度聚合统计。
     *
     * @param brandId   过滤品牌（可空）
     * @param typeId    过滤委托类型（可空）
     * @param startDate 起始日期（基于 created_at，可空）
     * @param endDate   结束日期（可空）
     * @param groupBy   brand|type|month|item，默认 brand
     */
    public Map<String, Object> aggregate(String brandId, String typeId,
                                         LocalDate startDate, LocalDate endDate,
                                         String groupBy) {
        if (groupBy == null || groupBy.isBlank()) groupBy = GROUP_BY_BRAND;

        LambdaQueryWrapper<Request> wrapper = new LambdaQueryWrapper<>();
        if (brandId != null) wrapper.eq(Request::getBrandId, brandId);
        if (typeId != null) wrapper.eq(Request::getTypeId, typeId);
        if (startDate != null) wrapper.ge(Request::getCreatedAt, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(Request::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        List<Request> requests = requestMapper.selectList(wrapper);

        BigDecimal totalCost = sumCost(requests);

        // Build ID-to-name lookup maps for brand and type dimensions
        Map<String, String> brandNames = brandMapper.selectList(null).stream()
                .collect(Collectors.toMap(Brand::getId, b -> nz(b.getName())));
        Map<String, String> typeNames = requestTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(RequestType::getId, t -> nz(t.getName())));

        Map<String, BigDecimal> grouped = switch (groupBy) {
            case GROUP_BY_TYPE -> requests.stream()
                    .filter(r -> r.getTotalCost() != null)
                    .collect(Collectors.groupingBy(r -> typeNames.getOrDefault(nz(r.getTypeId()), nz(r.getTypeId())),
                            Collectors.reducing(BigDecimal.ZERO, Request::getTotalCost, BigDecimal::add)));
            case GROUP_BY_MONTH -> requests.stream()
                    .filter(r -> r.getTotalCost() != null && r.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(
                            r -> ((LocalDateTime) r.getCreatedAt()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            TreeMap::new,
                            Collectors.reducing(BigDecimal.ZERO, Request::getTotalCost, BigDecimal::add)));
            case GROUP_BY_ITEM -> aggregateByItem(requests);
            default -> requests.stream()
                    .filter(r -> r.getTotalCost() != null)
                    .collect(Collectors.groupingBy(r -> brandNames.getOrDefault(nz(r.getBrandId()), nz(r.getBrandId())),
                            Collectors.reducing(BigDecimal.ZERO, Request::getTotalCost, BigDecimal::add)));
        };

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupBy", groupBy);
        result.put("totalCost", totalCost);
        result.put("requestCount", requests.size());
        result.put("costByBrand", grouped);
        return result;
    }

    /**
     * 按 AnalysisItem 维度：汇总每个 item 在所选 request 范围内的累计成本。
     */
    private Map<String, BigDecimal> aggregateByItem(List<Request> requests) {
        if (requests.isEmpty()) return Map.of();
        List<String> requestIds = requests.stream().map(Request::getId).toList();

        List<AnalysisTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<AnalysisTask>().in(AnalysisTask::getRequestId, requestIds));
        if (tasks.isEmpty()) return Map.of();

        Set<String> itemIds = tasks.stream().map(AnalysisTask::getItemId).collect(Collectors.toSet());
        Map<String, AnalysisItem> itemMap = itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(AnalysisItem::getId, e -> e));

        Map<String, BigDecimal> agg = new LinkedHashMap<>();
        for (AnalysisTask t : tasks) {
            AnalysisItem item = itemMap.get(t.getItemId());
            if (item == null || item.getCost() == null) continue;
            String label = item.getName() != null ? item.getName() : t.getItemId();
            agg.merge(label, item.getCost(), BigDecimal::add);
        }
        // 按金额降序
        return agg.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 导出 xlsx：第一页是聚合维度，第二页是明细 request 列表。
     */
    public byte[] exportXlsx(String brandId, String typeId,
                             LocalDate startDate, LocalDate endDate,
                             String groupBy) {
        Map<String, Object> agg = aggregate(brandId, typeId, startDate, endDate, groupBy);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet 1 : Summary
            Sheet sum = wb.createSheet("Summary");
            Row header = sum.createRow(0);
            header.createCell(0).setCellValue("Group (" + agg.get("groupBy") + ")");
            header.createCell(1).setCellValue("Cost");
            int r = 1;
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> data = (Map<String, BigDecimal>) agg.get("costByBrand");
            for (Map.Entry<String, BigDecimal> e : data.entrySet()) {
                Row row = sum.createRow(r++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue() == null ? 0 : e.getValue().doubleValue());
            }
            Row total = sum.createRow(r);
            total.createCell(0).setCellValue("TOTAL");
            BigDecimal totalCost = (BigDecimal) agg.get("totalCost");
            total.createCell(1).setCellValue(totalCost == null ? 0 : totalCost.doubleValue());
            sum.setColumnWidth(0, 8000);
            sum.setColumnWidth(1, 4000);

            // Sheet 2 : Detail
            LambdaQueryWrapper<Request> w = new LambdaQueryWrapper<>();
            if (brandId != null) w.eq(Request::getBrandId, brandId);
            if (typeId != null) w.eq(Request::getTypeId, typeId);
            if (startDate != null) w.ge(Request::getCreatedAt, startDate.atStartOfDay());
            if (endDate != null) w.lt(Request::getCreatedAt, endDate.plusDays(1).atStartOfDay());
            w.orderByDesc(Request::getCreatedAt);
            List<Request> requests = requestMapper.selectList(w);

            // Reuse name maps from aggregation
            Map<String, String> bNames = brandMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Brand::getId, b -> nz(b.getName())));
            Map<String, String> tNames = requestTypeMapper.selectList(null).stream()
                    .collect(Collectors.toMap(RequestType::getId, t -> nz(t.getName())));

            Sheet det = wb.createSheet("Detail");
            Row dh = det.createRow(0);
            String[] cols = {"RequestNo", "Brand", "Type", "PartNumber", "PartName", "Status", "TotalCost", "CreatedAt"};
            for (int i = 0; i < cols.length; i++) {
                dh.createCell(i).setCellValue(cols[i]);
            }
            int dr = 1;
            for (Request rq : requests) {
                Row row = det.createRow(dr++);
                row.createCell(0).setCellValue(nz(rq.getRequestNo()));
                row.createCell(1).setCellValue(bNames.getOrDefault(nz(rq.getBrandId()), nz(rq.getBrandId())));
                row.createCell(2).setCellValue(tNames.getOrDefault(nz(rq.getTypeId()), nz(rq.getTypeId())));
                row.createCell(3).setCellValue(nz(rq.getPartNumber()));
                row.createCell(4).setCellValue(nz(rq.getPartName()));
                row.createCell(5).setCellValue(nz(rq.getStatus()));
                row.createCell(6).setCellValue(rq.getTotalCost() == null ? 0 : rq.getTotalCost().doubleValue());
                row.createCell(7).setCellValue(rq.getCreatedAt() == null ? "" : rq.getCreatedAt().toString());
            }
            for (int i = 0; i < cols.length; i++) det.setColumnWidth(i, 4500);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export cost xlsx", e);
            throw new RuntimeException("Failed to export xlsx", e);
        }
    }

    private static BigDecimal sumCost(List<Request> list) {
        return list.stream()
                .map(Request::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
