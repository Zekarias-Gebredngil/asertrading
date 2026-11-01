package com.aser.asertrading.setting.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Corporate Profile
    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String amharicName;

    private String tinNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String phone;
    private String email;
    private String supportEmail;
    private String currency;

    // Inventory & Operational Policies
    @Column(nullable = false)
    private Integer lowStockThreshold;

    @Column(nullable = false)
    private Integer orderTimeoutHours;

    @Column(nullable = false)
    private Boolean autoFulfillReservations;

    @Column(nullable = false)
    private Boolean enforceStrictBranchLockdown;

    @Column(nullable = false)
    private String valuationMethod;

    // Security & Safeguards
    @Column(nullable = false)
    private Integer sessionTimeoutHours;

    @Column(nullable = false)
    private Integer minPasswordLength;

    @Column(nullable = false)
    private Boolean requireAuditLogging;

    @Column(columnDefinition = "TEXT")
    private String allowedOrigins;

    private Instant updatedAt;
    private String updatedBy;
}

