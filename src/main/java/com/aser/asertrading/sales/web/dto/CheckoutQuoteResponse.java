package com.aser.asertrading.sales.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteResponse(
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        boolean isStockAvailable,
        List<QuoteItemDetail> items,
        List<String> warnings
) {
    public record QuoteItemDetail(
            Long productId,
            String sku,
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            boolean isAvailable
    ) {}
}

