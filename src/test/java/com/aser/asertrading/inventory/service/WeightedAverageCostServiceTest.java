package com.aser.asertrading.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedAverageCostServiceTest {

    private final WeightedAverageCostService costService = new WeightedAverageCostService();

    @Test
    @DisplayName("SRS Section 7.1 example: 100m @ 50 ETB + 50m @ 60 ETB => 53.3333 ETB")
    void shouldCalculateWeightedAverageCostAccordingToSrsExample() {
        BigDecimal existingQuantity = BigDecimal.valueOf(100);
        BigDecimal existingCost = BigDecimal.valueOf(50);
        BigDecimal receivedQuantity = BigDecimal.valueOf(50);
        BigDecimal receivedCost = BigDecimal.valueOf(60);

        BigDecimal newAverageCost = costService.calculateNewAverageCost(
                existingQuantity, existingCost, receivedQuantity, receivedCost
        );

        // (100 * 50 + 50 * 60) / 150 = 8000 / 150 = 53.3333
        assertThat(newAverageCost).isEqualByComparingTo("53.3333");
    }

    @Test
    @DisplayName("When existing quantity is zero, new average cost equals received unit cost")
    void shouldReturnReceivedCostWhenExistingStockIsZero() {
        BigDecimal existingQuantity = BigDecimal.ZERO;
        BigDecimal existingCost = BigDecimal.ZERO;
        BigDecimal receivedQuantity = BigDecimal.valueOf(40);
        BigDecimal receivedCost = BigDecimal.valueOf(125.50);

        BigDecimal newAverageCost = costService.calculateNewAverageCost(
                existingQuantity, existingCost, receivedQuantity, receivedCost
        );

        assertThat(newAverageCost).isEqualByComparingTo("125.5000");
    }

    @Test
    @DisplayName("When received quantity is zero, existing cost is preserved")
    void shouldPreserveExistingCostWhenReceivedQuantityIsZero() {
        BigDecimal existingQuantity = BigDecimal.valueOf(200);
        BigDecimal existingCost = BigDecimal.valueOf(85.75);
        BigDecimal receivedQuantity = BigDecimal.ZERO;
        BigDecimal receivedCost = BigDecimal.valueOf(99.00);

        BigDecimal newAverageCost = costService.calculateNewAverageCost(
                existingQuantity, existingCost, receivedQuantity, receivedCost
        );

        assertThat(newAverageCost).isEqualByComparingTo("85.7500");
    }
}

