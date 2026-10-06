package com.lims.service.sharepoint;

import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;

public class SharePointException extends BusinessException {
    public SharePointException(String message) {
        super(ErrorCode.M365_INTEGRATION_ERROR, message);
    }

    public SharePointException(String message, Throwable cause) {
        super(ErrorCode.M365_INTEGRATION_ERROR, message);
        initCause(cause);
    }
}