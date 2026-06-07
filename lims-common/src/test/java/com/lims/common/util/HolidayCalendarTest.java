package com.lims.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HolidayCalendarTest {

    private static final Set<LocalDate> HOLIDAYS = Set.of(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 5, 1)
    );

    @Test
    @DisplayName("weekday in a holiday-free week is a business day")
    void weekdayInPlainWeek() {
        assertThat(HolidayCalendar.isBusinessDay(LocalDate.of(2026, 1, 6), HOLIDAYS)).isTrue();
    }

    @Test
    @DisplayName("Saturday is not a business day")
    void saturdayIsNotBusinessDay() {
        assertThat(HolidayCalendar.isBusinessDay(LocalDate.of(2026, 1, 3), HOLIDAYS)).isFalse();
    }

    @Test
    @DisplayName("a weekday on a national holiday is not a business day")
    void holidayOnWeekdayIsNotBusinessDay() {
        assertThat(HolidayCalendar.isBusinessDay(LocalDate.of(2026, 5, 1), HOLIDAYS)).isFalse();
    }

    @Test
    @DisplayName("addBusinessDays skips weekends and holidays")
    void addBusinessDaysSkipsNonBusinessDays() {
        // 2026-04-30 (Thu) + 2 business days → 2026-05-05 (Tue),
        // skipping 5/1 (Fri holiday) and 5/2-5/3 (weekend); 5/4 (Mon) is the 1st business day.
        LocalDate result = HolidayCalendar.addBusinessDays(LocalDate.of(2026, 4, 30), 2, HOLIDAYS);
        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 5));
    }

    @Test
    @DisplayName("addBusinessDays with days=0 returns the base date unchanged")
    void addBusinessDaysZeroReturnsBase() {
        assertThat(HolidayCalendar.addBusinessDays(LocalDate.of(2026, 4, 30), 0, HOLIDAYS))
                .isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("addBusinessDays rejects negative day counts")
    void addBusinessDaysRejectsNegative() {
        assertThatThrownBy(() -> HolidayCalendar.addBusinessDays(LocalDate.of(2026, 4, 30), -1, HOLIDAYS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("countBusinessDays counts only weekdays not in the holiday set")
    void countBusinessDays() {
        // 2026-04-27 (Mon) to 2026-05-05 (Tue): 9 calendar days, 7 weekdays, 1 holiday = 6
        int count = HolidayCalendar.countBusinessDays(
                LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 5), HOLIDAYS);
        assertThat(count).isEqualTo(6);
    }
}
