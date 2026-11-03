package com.aser.asertrading.procurement.web.dto;

import com.aser.asertrading.procurement.domain.POStatus;
import com.aser.asertrading.procurement.domain.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderDto(
        Long id,
        String poNumber,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        POStatus status,
        BigDecimal totalAmount,
        String notes,
        String createdBy,
        String approvedBy,
        List<POItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static PurchaseOrderDto of(PurchaseOrder po, String supplierName, String warehouseName, List<POItemDto> items) {
        return new PurchaseOrderDto(
                po.getId(),
                po.getPoNumber(),
                po.getSupplierId(),
                supplierName,
                po.getWarehouseId(),
                warehouseName,
                po.getStatus(),
                po.getTotalAmount(),
                po.getNotes(),
                po.getCreatedBy(),
                po.getApprovedBy(),
                items,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}

