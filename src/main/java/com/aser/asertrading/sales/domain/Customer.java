package com.aser.asertrading.sales.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_email", columnList = "email"),
        @Index(name = "idx_customers_phone", columnList = "phone"),
        @Index(name = "idx_customers_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(length = 60)
    private String city;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

