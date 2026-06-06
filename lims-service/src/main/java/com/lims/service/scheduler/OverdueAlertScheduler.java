package com.lims.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 委托逾期/临期告警扫描器：每小时统计仍在进行中的 Request 距 due_date 的剩余天数。
 *
 * 告警等级：
 * - RED   : 已过期 (剩余 < 0)
 * - ORANGE: 1 - 2 天
 * - YELLOW: 3 - 5 天
 * - GREEN : > 5 天（不告警）
 *
 * 当前实现先输出统计日志；后续可扩展为：
 *  - 写入 sys_message 表
 *  - 推送邮件 / Teams Webhook
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueAlertScheduler {

    private static final List<String> ACTIVE_STATUSES = List.of(
            "SUBMITTED", "ASSIGNED", "SAMPLING", "REPORTING", "APPROVING");

    private final RequestMapper requestMapper;

    @Scheduled(cron = "0 0 * * * ?")
    public void scanDueDates() {
        LocalDate today = LocalDate.now();
        List<Request> active = requestMapper.selectList(
                new LambdaQueryWrapper<Request>()
                        .in(Request::getStatus, ACTIVE_STATUSES)
                        .isNotNull(Request::getDueDate));

        Map<String, Integer> counters = new HashMap<>();
        counters.put("RED", 0);
        counters.put("ORANGE", 0);
        counters.put("YELLOW", 0);
        counters.put("GREEN", 0);

        for (Request r : active) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, r.getDueDate());
            String level;
            if (days < 0) level = "RED";
            else if (days <= 2) level = "ORANGE";
            else if (days <= 5) level = "YELLOW";
            else level = "GREEN";
            counters.merge(level, 1, Integer::sum);
            if ("RED".equals(level) || "ORANGE".equals(level)) {
                log.info("Due-date alert [{}] requestNo={} dueDate={} requesterId={}",
                        level, r.getRequestNo(), r.getDueDate(), r.getRequesterId());
            }
        }
        log.info("Due-date scan finished. RED={}, ORANGE={}, YELLOW={}, GREEN={}, total={}",
                counters.get("RED"), counters.get("ORANGE"),
                counters.get("YELLOW"), counters.get("GREEN"), active.size());
    }
}
