package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.ReturnAction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateReturnRequest(
        @NotBlank(message = "Return reason is required")
        String reason,

        String notes,

        @NotEmpty(message = "Return must include items")
        List<ReturnItemRequest> items
) {
    public record ReturnItemRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.0001") BigDecimal quantity,
            ReturnAction action
    ) {}
}

