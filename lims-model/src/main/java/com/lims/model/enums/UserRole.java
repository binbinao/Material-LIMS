package com.lims.model.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    REQUESTER("REQUESTER"),
    TECHNICIAN("TECHNICIAN"),
    ENGINEER("ENGINEER"),
    MANAGER("MANAGER"),
    ADMIN("ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }
}
