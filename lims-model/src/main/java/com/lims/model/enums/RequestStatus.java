package com.lims.model.enums;

import lombok.Getter;

@Getter
public enum RequestStatus {
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    ASSIGNED("ASSIGNED"),
    SAMPLING("SAMPLING"),
    REPORTING("REPORTING"),
    APPROVING("APPROVING"),
    COMPLETED("COMPLETED"),
    REJECTED("REJECTED");

    private final String value;

    RequestStatus(String value) {
        this.value = value;
    }
}
