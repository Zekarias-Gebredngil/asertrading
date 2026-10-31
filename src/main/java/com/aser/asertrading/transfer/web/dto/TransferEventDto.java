package com.aser.asertrading.transfer.web.dto;

import com.aser.asertrading.transfer.domain.TransferEvent;

import java.time.Instant;

public record TransferEventDto(
        Long id,
        String eventType,
        String actor,
        String notes,
        Instant createdAt
) {
    public static TransferEventDto fromEntity(TransferEvent event) {
        return new TransferEventDto(
                event.getId(),
                event.getEventType(),
                event.getActor(),
                event.getNotes(),
                event.getCreatedAt()
        );
    }
}

