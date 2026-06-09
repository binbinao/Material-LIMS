package com.lims.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HolidayCalendar 工作日 / 节假日计算")
class HolidayCalendarTest {

    // 2024-01-01 周一, 2024-01-06 周六, 2024-01-07 周日
    private static final LocalDate MON = LocalDate.of(2024, 1, 1);
    private static final LocalDate FRI = LocalDate.of(2024, 1, 5);
    private static final LocalDate SAT = LocalDate.of(2024, 1, 6);
    private static final LocalDate SUN = LocalDate.of(2024, 1, 7);

    @Nested
    @DisplayName("isBusinessDay")
    class IsBusinessDay {

        @Test
        @DisplayName("工作日(周一)返回 true")
        void weekdayIsBusinessDay() {
            assertThat(HolidayCalendar.isBusinessDay(MON, Set.of())).isTrue();
        }

        @Test
        @DisplayName("周六/周日返回 false")
        void weekendIsNotBusinessDay() {
            assertThat(HolidayCalendar.isBusinessDay(SAT, Set.of())).isFalse();
            assertThat(HolidayCalendar.isBusinessDay(SUN, Set.of())).isFalse();
        }

        @Test
        @DisplayName("命中节假日集合的工作日返回 false")
        void holidayIsNotBusinessDay() {
            assertThat(HolidayCalendar.isBusinessDay(MON, Set.of(MON))).isFalse();
        }

        @Test
        @DisplayName("holidays 为 null 时仅按星期判定")
        void nullHolidaysFallsBackToWeekday() {
            assertThat(HolidayCalendar.isBusinessDay(MON, null)).isTrue();
            assertThat(HolidayCalendar.isBusinessDay(SAT, null)).isFalse();
        }

        @Test
        @DisplayName("date 为 null 返回 false(不抛异常)")
        void nullDateReturnsFalse() {
            assertThat(HolidayCalendar.isBusinessDay(null, Set.of())).isFalse();
        }
    }

    @Nested
    @DisplayName("addBusinessDays")
    class AddBusinessDays {

        @Test
        @DisplayName("days=0 原样返回 base(即使 base 是周末)")
        void zeroDaysReturnsBase() {
            assertThat(HolidayCalendar.addBusinessDays(SAT, 0, Set.of())).isEqualTo(SAT);
        }

        @Test
        @DisplayName("跨周末跳过非工作日：周五 +1 个工作日 = 下周一")
        void skipsWeekend() {
            assertThat(HolidayCalendar.addBusinessDays(FRI, 1, Set.of()))
                    .isEqualTo(LocalDate.of(2024, 1, 8));
        }

        @Test
        @DisplayName("跨节假日跳过：周一 +1，若周二是节假日则落到周三")
        void skipsHoliday() {
            LocalDate tue = LocalDate.of(2024, 1, 2);
            LocalDate wed = LocalDate.of(2024, 1, 3);
            assertThat(HolidayCalendar.addBusinessDays(MON, 1, Set.of(tue))).isEqualTo(wed);
        }

        @Test
        @DisplayName("base 为 null 抛 IllegalArgumentException")
        void nullBaseThrows() {
            assertThatThrownBy(() -> HolidayCalendar.addBusinessDays(null, 1, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("base");
        }

        @Test
        @DisplayName("days 为负数抛 IllegalArgumentException")
        void negativeDaysThrows() {
            assertThatThrownBy(() -> HolidayCalendar.addBusinessDays(MON, -1, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-negative");
        }
    }

    @Nested
    @DisplayName("countBusinessDays")
    class CountBusinessDays {

        @Test
        @DisplayName("闭区间含两端：周一至周五共 5 个工作日")
        void countsInclusiveRange() {
            assertThat(HolidayCalendar.countBusinessDays(MON, FRI, Set.of())).isEqualTo(5);
        }

        @Test
        @DisplayName("扣除周末：整周(周一至周日)仍为 5")
        void excludesWeekend() {
            assertThat(HolidayCalendar.countBusinessDays(MON, SUN, Set.of())).isEqualTo(5);
        }

        @Test
        @DisplayName("扣除节假日：整周内有一个节假日则为 4")
        void excludesHoliday() {
            assertThat(HolidayCalendar.countBusinessDays(MON, FRI, Set.of(LocalDate.of(2024, 1, 3))))
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("from 等于 to 且为工作日时为 1")
        void sameDayBusiness() {
            assertThat(HolidayCalendar.countBusinessDays(MON, MON, Set.of())).isEqualTo(1);
        }

        @Test
        @DisplayName("from 晚于 to 返回 0")
        void reversedRangeReturnsZero() {
            assertThat(HolidayCalendar.countBusinessDays(FRI, MON, Set.of())).isZero();
        }

        @Test
        @DisplayName("holidays 为 null 不影响计数")
        void nullHolidaysCounts() {
            assertThat(HolidayCalendar.countBusinessDays(MON, FRI, null)).isEqualTo(5);
        }

        @Test
        @DisplayName("from 或 to 为 null 抛 IllegalArgumentException")
        void nullBoundsThrow() {
            assertThatThrownBy(() -> HolidayCalendar.countBusinessDays(null, FRI, Collections.emptySet()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> HolidayCalendar.countBusinessDays(MON, null, Collections.emptySet()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
