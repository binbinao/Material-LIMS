package com.lims.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.model.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * Largest numeric suffix among the "rpt-NNN" ids in the report
     * table, or 0 if none exist. Used by ReportService.createReport()
     * to mint the next sequential id.
     *
     * The regex anchors to a pure-integer suffix so the hand-curated
     * "rpt-edge-001" rows from the V7 seed migration are ignored —
     * those belong to a separate namespace and should never collide
     * with the auto-generated rpt-NNN counter.
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM 5) AS INTEGER)), 0) FROM report WHERE id ~ '^rpt-[0-9]+$'")
    int selectMaxNumericReportId();
}
