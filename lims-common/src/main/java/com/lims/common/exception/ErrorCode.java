package com.lims.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // General errors (1000-1999)
    PARAM_VALIDATION_FAILED(1001, "Parameter validation failed"),
    DATA_NOT_FOUND(1002, "Data not found"),
    DATA_ALREADY_EXISTS(1003, "Data already exists"),
    OPERATION_NOT_ALLOWED(1004, "Operation not allowed"),

    // Business errors (2000-2999)
    REQUEST_STATUS_INVALID(2001, "Request status does not allow this operation"),
    REPORT_VERSION_CONFLICT(2002, "Report version conflict"),
    DUE_DATE_PASSED(2003, "Due date has passed"),
    REPORT_NOT_EDITABLE(2004, "Report is not editable in current status"),
    ASSIGNMENT_REQUIRED(2005, "Engineer assignment is required"),
    REVISION_NOTE_REQUIRED(2006, "Revision note is required for report revision"),

    // Auth errors (3000-3999)
    UNAUTHORIZED(3001, "Unauthorized"),
    ACCESS_DENIED(3002, "Access denied"),
    TOKEN_EXPIRED(3003, "Token expired"),

    // System errors (5000-5999)
    EXTERNAL_API_UNAVAILABLE(5001, "External API unavailable"),
    FILE_CONVERT_FAILED(5002, "File conversion failed"),
    M365_INTEGRATION_ERROR(5003, "Microsoft 365 integration error"),
    FILE_UPLOAD_FAILED(5004, "File upload failed"),
    DATA_PERMISSION_FILTER_FAILED(5005, "Row-level data permission filter could not be applied"),
    LOGIN_PROVIDER_UNAVAILABLE(5006, "Login provider temporarily unavailable");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
