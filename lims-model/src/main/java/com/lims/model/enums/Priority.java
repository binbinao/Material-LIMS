package com.lims.model.enums;

import lombok.Getter;

@Getter
public enum Priority {
    LOW("LOW"),
    NORMAL("NORMAL"),
    HIGH("HIGH"),
    URGENT("URGENT");

    private final String value;

    Priority(String value) {
        this.value = value;
    }
}
