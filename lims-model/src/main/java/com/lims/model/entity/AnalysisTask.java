package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("analysis_task")
public class AnalysisTask extends BaseEntity {

    private String requestId;
    private String itemId;
    private String assigneeId;
    private String status;
    private String delayReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer sortOrder;
}
