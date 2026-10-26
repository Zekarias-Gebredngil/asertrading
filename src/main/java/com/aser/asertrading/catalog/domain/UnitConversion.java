package com.aser.asertrading.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "unit_conversions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_unit_conversion", columnNames = {"from_unit_id", "to_unit_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_unit_id", nullable = false)
    private UnitOfMeasure fromUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_unit_id", nullable = false)
    private UnitOfMeasure toUnit;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal multiplier;
}

