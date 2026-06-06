package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_doc")
public class KnowledgeDoc extends BaseEntity {

    /** Document title */
    private String title;

    /** Category: MANUAL or VIDEO */
    private String category;

    /** Storage URL (MinIO presigned or file:// fallback) */
    private String fileUrl;

    /** File size in bytes */
    private Long fileSize;

    /** Optional description */
    private String description;
}
