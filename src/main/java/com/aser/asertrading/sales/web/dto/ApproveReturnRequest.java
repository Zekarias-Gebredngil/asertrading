package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.ReturnAction;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApproveReturnRequest(
        String inspectionNotes,
        boolean refundPayment,
        List<ItemInspectionDecision> itemDecisions
) {
    public record ItemInspectionDecision(
            @NotNull Long returnItemId,
            @NotNull ReturnAction action // RESTOCK, QUARANTINE, SCRAP
    ) {}
}

