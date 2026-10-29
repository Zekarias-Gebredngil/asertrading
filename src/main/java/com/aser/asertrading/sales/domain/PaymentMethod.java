package com.aser.asertrading.sales.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
    CASH,
    MANUAL_TRANSFER,
    TELEBIRR_FUTURE;

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        if (value == null || value.isBlank()) {
            return CASH;
        }
        return switch (value.toUpperCase().trim()) {
            case "CASH", "CASH_ON_DELIVERY", "IN_HAND_CASH" -> CASH;
            case "MANUAL_TRANSFER", "MANUAL_BANK_TRANSFER", "BANK_TRANSFER" -> MANUAL_TRANSFER;
            case "TELEBIRR", "TELEBIRR_FUTURE", "MOBILE_MONEY" -> TELEBIRR_FUTURE;
            default -> {
                try {
                    yield PaymentMethod.valueOf(value.toUpperCase().trim());
                } catch (IllegalArgumentException e) {
                    yield CASH;
                }
            }
        };
    }
}
