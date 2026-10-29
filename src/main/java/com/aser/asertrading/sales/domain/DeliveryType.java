package com.aser.asertrading.sales.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DeliveryType {
    DELIVERY,
    COLLECTION;

    @JsonCreator
    public static DeliveryType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DELIVERY;
        }
        return switch (value.toUpperCase().trim()) {
            case "STANDARD_DELIVERY", "DELIVERY", "HOME_DELIVERY", "EXPRESS_DELIVERY" -> DELIVERY;
            case "STORE_PICKUP", "COLLECTION", "PICKUP", "BRANCH_PICKUP" -> COLLECTION;
            default -> {
                try {
                    yield DeliveryType.valueOf(value.toUpperCase().trim());
                } catch (IllegalArgumentException e) {
                    yield DELIVERY;
                }
            }
        };
    }
}
