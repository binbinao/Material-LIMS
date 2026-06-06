package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.util.HolidayCalendar;
import com.lims.dao.mapper.HolidayMapper;
import com.lims.model.entity.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayService {

    private final HolidayMapper holidayMapper;

    public Page<Holiday> list(int page, int size, Integer year) {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(Holiday::getYear, year);
        }
        wrapper.orderByAsc(Holiday::getDate);
        long current = page <= 0 ? 1 : page;
        return holidayMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<Holiday> listByYear(int year) {
        return holidayMapper.selectList(
                new LambdaQueryWrapper<Holiday>().eq(Holiday::getYear, year).orderByAsc(Holiday::getDate));
    }

    public Holiday getById(String id) {
        return holidayMapper.selectById(id);
    }

    /**
     * 加载某年节假日日期集合，结果缓存到 Redis（cacheName = holidaysByYear，key = year）。
     */
    @Cacheable(value = "holidaysByYear", key = "#year")
    public Set<LocalDate> getHolidayDates(int year) {
        List<Holiday> all = holidayMapper.selectList(
                new LambdaQueryWrapper<Holiday>()
                        .eq(Holiday::getYear, year)
                        .in(Holiday::getType, "NATIONAL", "COMPANY"));
        Set<LocalDate> set = all.stream().map(Holiday::getDate).collect(Collectors.toCollection(HashSet::new));
        log.debug("Loaded {} holidays for year {}", set.size(), year);
        return set;
    }

    /**
     * 跨年场景：返回 from..to 涉及的所有年份的节假日并集。
     */
    public Set<LocalDate> getHolidayDatesBetween(LocalDate from, LocalDate to) {
        Set<LocalDate> merged = new HashSet<>();
        for (int y = from.getYear(); y <= to.getYear(); y++) {
            merged.addAll(getHolidayDates(y));
        }
        return merged;
    }

    /**
     * 在 base 后跳过 days 个工作日。会自动覆盖跨年情况。
     */
    public LocalDate addBusinessDays(LocalDate base, int days) {
        // 上限按 days 折算近似覆盖年份（保守 +2 倍 + 365 防御）
        LocalDate upper = base.plusDays(((long) days) * 2L + 365L);
        Set<LocalDate> holidays = getHolidayDatesBetween(base, upper);
        return HolidayCalendar.addBusinessDays(base, days, holidays);
    }

    public boolean isBusinessDay(LocalDate date) {
        return HolidayCalendar.isBusinessDay(date, getHolidayDates(date.getYear()));
    }

    public int countBusinessDays(LocalDate from, LocalDate to) {
        return HolidayCalendar.countBusinessDays(from, to, getHolidayDatesBetween(from, to));
    }

    @CacheEvict(value = "holidaysByYear", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Holiday create(Holiday entity) {
        holidayMapper.insert(entity);
        return entity;
    }

    @CacheEvict(value = "holidaysByYear", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void batchImport(List<Holiday> holidays) {
        for (Holiday h : holidays) {
            holidayMapper.insert(h);
        }
        log.info("Imported {} holidays", holidays.size());
    }

    @CacheEvict(value = "holidaysByYear", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Holiday update(String id, Holiday entity) {
        entity.setId(id);
        holidayMapper.updateById(entity);
        return entity;
    }

    @CacheEvict(value = "holidaysByYear", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        holidayMapper.deleteById(id);
    }
}
