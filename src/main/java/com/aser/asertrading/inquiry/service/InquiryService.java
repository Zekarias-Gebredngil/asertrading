package com.aser.asertrading.inquiry.service;

import java.math.BigDecimal;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inquiry.domain.CustomerInquiry;
import com.aser.asertrading.inquiry.domain.InquiryStatus;
import com.aser.asertrading.inquiry.domain.InquiryType;
import com.aser.asertrading.inquiry.repository.CustomerInquiryRepository;
import com.aser.asertrading.inquiry.web.dto.CreateInquiryRequest;
import com.aser.asertrading.inquiry.web.dto.CustomerInquiryDto;
import com.aser.asertrading.inquiry.web.dto.QuoteInquiryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final CustomerInquiryRepository inquiryRepository;
    private final AuditService auditService;

    @Transactional
    public CustomerInquiryDto submitInquiry(CreateInquiryRequest request, String clientIp) {
        String inquiryNumber = "INQ-" + System.currentTimeMillis() % 10000000;

        CustomerInquiry inquiry = CustomerInquiry.builder()
                .inquiryNumber(inquiryNumber)
                .customerName(request.customerName().trim())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .companyName(request.companyName() != null ? request.companyName().trim() : null)
                .serviceType(request.serviceType() != null ? request.serviceType().trim() : "General")
                .inquiryType(request.inquiryType() != null ? request.inquiryType() : InquiryType.GENERAL_INQUIRY)
                .subject(request.subject().trim())
                .message(request.message().trim())
                .specifications(request.specifications())
                .requestedQuantity(request.requestedQuantity())
                .status(InquiryStatus.NEW)
                .build();

        inquiry = inquiryRepository.save(inquiry);

        auditService.log(inquiry.getEmail(), "SUBMIT_INQUIRY", "CustomerInquiry", inquiry.getId().toString(),
                "Submitted " + inquiry.getInquiryType() + " #" + inquiry.getInquiryNumber() + " for " + inquiry.getServiceType(), clientIp);

        return CustomerInquiryDto.fromEntity(inquiry);
    }

    @Transactional
    public CustomerInquiryDto quoteInquiry(Long id, QuoteInquiryRequest request, String actor) {
        CustomerInquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer inquiry not found with id: " + id));

        inquiry.setQuotedPrice(request.quotedPrice());
        inquiry.setEstimatedCost(request.estimatedCost() != null ? request.estimatedCost() : BigDecimal.ZERO);
        inquiry.setAdminNotes(request.adminNotes());
        inquiry.setStatus(request.status() != null ? request.status() : InquiryStatus.QUOTE_SENT);
        inquiry.setAssignedTo(actor);

        inquiry = inquiryRepository.save(inquiry);

        auditService.log(actor, "QUOTE_INQUIRY", "CustomerInquiry", inquiry.getId().toString(),
                "Provided quotation of " + request.quotedPrice() + " ETB for inquiry " + inquiry.getInquiryNumber(), null);

        return CustomerInquiryDto.fromEntity(inquiry);
    }

    @Transactional(readOnly = true)
    public Page<CustomerInquiryDto> getInquiries(InquiryStatus status, InquiryType type, Pageable pageable) {
        Page<CustomerInquiry> page;
        if (status != null) {
            page = inquiryRepository.findByStatus(status, pageable);
        } else if (type != null) {
            page = inquiryRepository.findByInquiryType(type, pageable);
        } else {
            page = inquiryRepository.findAll(pageable);
        }
        return page.map(CustomerInquiryDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public CustomerInquiryDto getInquiryById(Long id) {
        CustomerInquiry inq = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found with id: " + id));
        return CustomerInquiryDto.fromEntity(inq);
    }
}

