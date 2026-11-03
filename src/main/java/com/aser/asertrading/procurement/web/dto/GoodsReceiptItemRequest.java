package com.aser.asertrading.procurement.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GoodsReceiptItemRequest(
        @NotNull(message = "Purchase order item ID is required")
        Long purchaseOrderItemId,

        @NotNull(message = "Quantity received is required")
        @DecimalMin(value = "0.0001", message = "Quantity received must be greater than zero")
        BigDecimal quantityReceived,

        BigDecimal unitCost
) {}

