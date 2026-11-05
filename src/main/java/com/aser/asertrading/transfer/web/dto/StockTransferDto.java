package com.aser.asertrading.transfer.web.dto;

import com.aser.asertrading.transfer.domain.StockTransfer;
import com.aser.asertrading.transfer.domain.TransferStatus;

import java.time.Instant;
import java.util.List;

public record StockTransferDto(
        Long id,
        String transferNumber,
        Long sourceWarehouseId,
        String sourceWarehouseName,
        Long sourceBranchId,
        String sourceBranchName,
        Long destinationWarehouseId,
        String destinationWarehouseName,
        Long destinationBranchId,
        String destinationBranchName,
        TransferStatus status,
        String requestedBy,
        String approvedBy,
        String dispatchedBy,
        String receivedBy,
        String rejectionReason,
        String notes,
        List<TransferItemDto> items,
        List<TransferEventDto> events,
        Instant createdAt,
        Instant updatedAt
) {
    public static StockTransferDto of(StockTransfer t,
                                      String sourceWhName, Long sourceBrId, String sourceBrName,
                                      String destWhName, Long destBrId, String destBrName,
                                      List<TransferItemDto> items, List<TransferEventDto> events) {
        return new StockTransferDto(
                t.getId(),
                t.getTransferNumber(),
                t.getSourceWarehouseId(),
                sourceWhName,
                sourceBrId,
                sourceBrName,
                t.getDestinationWarehouseId(),
                destWhName,
                destBrId,
                destBrName,
                t.getStatus(),
                t.getRequestedBy(),
                t.getApprovedBy(),
                t.getDispatchedBy(),
                t.getReceivedBy(),
                t.getRejectionReason(),
                t.getNotes(),
                items,
                events,
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
