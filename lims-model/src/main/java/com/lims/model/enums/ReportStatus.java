package com.lims.model.enums;

import lombok.Getter;

@Getter
public enum ReportStatus {
    DRAFT("DRAFT"),
    IN_REVIEW("IN_REVIEW"),
    APPROVED("APPROVED"),
    REVISING("REVISING");

    private final String value;

    ReportStatus(String value) {
        this.value = value;
    }
}
