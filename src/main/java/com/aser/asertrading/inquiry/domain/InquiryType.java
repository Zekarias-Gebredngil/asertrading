package com.aser.asertrading.inquiry.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum InquiryType {
    GENERAL_INQUIRY,
    QUOTATION_REQUEST,
    PRICE_QUOTATION,
    CUSTOM_PRINT_JOB,
    FLEET_BRANDING,
    BULK_MATERIAL_SUPPLY;

    @JsonCreator
    public static InquiryType fromString(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL_INQUIRY;
        }
        String normalized = value.trim().toUpperCase();
        for (InquiryType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        if ("QUOTE".equalsIgnoreCase(normalized) || "QUOTATION".equalsIgnoreCase(normalized)) {
            return QUOTATION_REQUEST;
        }
        return GENERAL_INQUIRY;
    }
}

