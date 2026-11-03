package com.aser.asertrading.procurement.web.dto;

import com.aser.asertrading.procurement.domain.POStatus;
import com.aser.asertrading.procurement.domain.PurchaseOrder;
import com.aser.asertrading.procurement.domain.PurchaseOrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record POItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        BigDecimal receivedQuantity
) {
    public static POItemDto of(PurchaseOrderItem item, String sku, String name) {
        return new POItemDto(
                item.getId(),
                item.getProductId(),
                sku,
                name,
                item.getQuantity(),
                item.getUnitCost(),
                item.getTotalCost(),
                item.getReceivedQuantity()
        );
    }
}

