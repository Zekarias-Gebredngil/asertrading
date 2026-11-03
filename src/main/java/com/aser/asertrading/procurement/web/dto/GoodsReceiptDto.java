package com.aser.asertrading.procurement.web.dto;

import com.aser.asertrading.procurement.domain.GoodsReceipt;
import com.aser.asertrading.procurement.domain.GoodsReceiptItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record GoodsReceiptDto(
        Long id,
        String grnNumber,
        Long purchaseOrderId,
        Long warehouseId,
        String warehouseName,
        String receivedBy,
        String deliveryNoteNumber,
        String notes,
        List<GoodsReceiptItemDto> items,
        Instant receivedAt
) {
    public static GoodsReceiptDto of(GoodsReceipt grn, String warehouseName, List<GoodsReceiptItemDto> items) {
        return new GoodsReceiptDto(
                grn.getId(),
                grn.getGrnNumber(),
                grn.getPurchaseOrderId(),
                grn.getWarehouseId(),
                warehouseName,
                grn.getReceivedBy(),
                grn.getDeliveryNoteNumber(),
                grn.getNotes(),
                items,
                grn.getReceivedAt()
        );
    }
}

