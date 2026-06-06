package com.lims.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 工作日 / 节假日计算工具。
 *
 * <p>不依赖 Spring 容器，可在任意层使用。需由调用方传入"该年节假日日期集合"，
 * 调用方负责从数据库加载并缓存（参见 lims-service 的 HolidayService）。
 */
public final class HolidayCalendar {

    private HolidayCalendar() {
    }

    /** 是否工作日：周一至周五且不在节假日集合内。 */
    public static boolean isBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        if (date == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return holidays == null || !holidays.contains(date);
    }

    /**
     * 在 base 日期之后跳过 days 个工作日，返回到达日期。
     *
     * <p>当 days = 0 时返回 base 本身（不做跳过）。
     */
    public static LocalDate addBusinessDays(LocalDate base, int days, Set<LocalDate> holidays) {
        if (base == null) {
            throw new IllegalArgumentException("base date must not be null");
        }
        if (days < 0) {
            throw new IllegalArgumentException("days must be non-negative");
        }
        LocalDate cur = base;
        int remaining = days;
        while (remaining > 0) {
            cur = cur.plusDays(1);
            if (isBusinessDay(cur, holidays)) {
                remaining--;
            }
        }
        return cur;
    }

    /** 闭区间 [from, to] 内的工作日数量。 */
    public static int countBusinessDays(LocalDate from, LocalDate to, Set<LocalDate> holidays) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to must not be null");
        }
        if (from.isAfter(to)) return 0;
        int count = 0;
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            if (isBusinessDay(cur, holidays)) count++;
            cur = cur.plusDays(1);
        }
        return count;
    }
}
