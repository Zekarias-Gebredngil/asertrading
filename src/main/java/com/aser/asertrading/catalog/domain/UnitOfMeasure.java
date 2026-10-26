package com.aser.asertrading.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "units_of_measure", indexes = {
        @Index(name = "idx_uom_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitOfMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 10)
    private String symbol;

    @Column(name = "is_base_unit", nullable = false)
    @Builder.Default
    private boolean isBaseUnit = true;
}

