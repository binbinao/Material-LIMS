package com.lims.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RequestCreateDTO {

    @NotBlank(message = "Brand is required")
    private String brandId;

    private String deptId;

    @NotBlank(message = "Request type is required")
    private String typeId;

    private String partNumber;
    private String partName;
    private String eco;
    private String supplierCode;
    private String supplierName;

    @NotBlank(message = "Request reason is required")
    private String requestReason;

    private String priority;

    /** Whether this is a proxy request (代下单) */
    private Boolean proxyRequest;

    /** Real requester name (for proxy requests) */
    private String realRequesterName;

    /** Selected analysis item IDs */
    @NotEmpty(message = "Please select at least one analysis item")
    private List<String> analysisItemIds;
}
