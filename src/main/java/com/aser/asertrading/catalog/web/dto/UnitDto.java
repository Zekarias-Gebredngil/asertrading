package com.aser.asertrading.catalog.web.dto;

import com.aser.asertrading.catalog.domain.UnitOfMeasure;

public record UnitDto(
        Long id,
        String name,
        String code,
        String symbol,
        boolean isBaseUnit
) {
    public static UnitDto fromEntity(UnitOfMeasure uom) {
        return new UnitDto(
                uom.getId(),
                uom.getName(),
                uom.getCode(),
                uom.getSymbol(),
                uom.isBaseUnit()
        );
    }
}

