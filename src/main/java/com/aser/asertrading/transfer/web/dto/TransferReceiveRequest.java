package com.aser.asertrading.transfer.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TransferReceiveRequest(
        String notes,

        @NotEmpty(message = "Received item list cannot be empty")
        @Valid
        List<TransferReceiveItemRequest> items
) {}

