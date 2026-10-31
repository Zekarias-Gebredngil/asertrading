package com.aser.asertrading.transfer.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTransferRequest(
        @NotNull(message = "Source warehouse is required")
        Long sourceWarehouseId,

        @NotNull(message = "Destination warehouse is required")
        Long destinationWarehouseId,

        String notes,

        @NotEmpty(message = "At least one item must be included in the transfer")
        @Valid
        List<TransferItemRequest> items
) {}

