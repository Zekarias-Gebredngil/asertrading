package com.aser.asertrading.procurement.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GoodsReceiptRequest(
        String deliveryNoteNumber,
        String notes,

        @NotEmpty(message = "Goods receipt must include received items")
        @Valid
        List<GoodsReceiptItemRequest> items
) {}

