package com.aser.asertrading.inquiry.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customer_inquiries", indexes = {
        @Index(name = "idx_inquiries_number", columnList = "inquiry_number", unique = true),
        @Index(name = "idx_inquiries_email", columnList = "email"),
        @Index(name = "idx_inquiries_status", columnList = "status"),
        @Index(name = "idx_inquiries_type", columnList = "inquiry_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_number", nullable = false, unique = true, length = 40)
    private String inquiryNumber;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "company_name", length = 120)
    private String companyName;

    @Column(name = "service_type", length = 100)
    private String serviceType; // Printing Press, Branding & Promotion, Garment Materials, Custom

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 30)
    @Builder.Default
    private InquiryType inquiryType = InquiryType.GENERAL_INQUIRY;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "requested_quantity", precision = 18, scale = 4)
    private BigDecimal requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "quoted_price", precision = 18, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "estimated_cost", precision = 18, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    public BigDecimal getNetRevenue() {
        if (quotedPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal cost = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
        BigDecimal net = quotedPrice.subtract(cost);
        return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

