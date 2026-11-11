package com.aser.asertrading.inquiry.web.dto;

import com.aser.asertrading.inquiry.domain.CustomerInquiry;
import com.aser.asertrading.inquiry.domain.InquiryStatus;
import com.aser.asertrading.inquiry.domain.InquiryType;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerInquiryDto(
        Long id,
        String inquiryNumber,
        String customerName,
        String email,
        String phone,
        String companyName,
        String serviceType,
        InquiryType inquiryType,
        String subject,
        String message,
        String specifications,
        BigDecimal requestedQuantity,
        InquiryStatus status,
        BigDecimal quotedPrice,
        BigDecimal estimatedCost,
        BigDecimal netRevenue,
        String adminNotes,
        String assignedTo,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerInquiryDto fromEntity(CustomerInquiry inq) {
        return new CustomerInquiryDto(
                inq.getId(),
                inq.getInquiryNumber(),
                inq.getCustomerName(),
                inq.getEmail(),
                inq.getPhone(),
                inq.getCompanyName(),
                inq.getServiceType(),
                inq.getInquiryType(),
                inq.getSubject(),
                inq.getMessage(),
                inq.getSpecifications(),
                inq.getRequestedQuantity(),
                inq.getStatus(),
                inq.getQuotedPrice(),
                inq.getEstimatedCost(),
                inq.getNetRevenue(),
                inq.getAdminNotes(),
                inq.getAssignedTo(),
                inq.getCreatedAt(),
                inq.getUpdatedAt()
        );
    }
}

