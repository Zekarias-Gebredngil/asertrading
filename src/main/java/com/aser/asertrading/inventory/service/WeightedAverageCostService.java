package com.aser.asertrading.inventory.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class WeightedAverageCostService {

    public static final int COST_SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculates the new weighted-average unit cost per Section 7 of SRS:
     * New Cost = (Existing Qty * Existing Cost + Received Qty * Received Unit Cost) / (Existing Qty + Received Qty)
     *
     * @param existingQuantity current on-hand quantity before receipt
     * @param existingCost current weighted average unit cost
     * @param receivedQuantity received quantity
     * @param receivedUnitCost unit cost of the received batch
     * @return new weighted average cost
     */
    public BigDecimal calculateNewAverageCost(
            BigDecimal existingQuantity,
            BigDecimal existingCost,
            BigDecimal receivedQuantity,
            BigDecimal receivedUnitCost
    ) {
        if (existingQuantity == null || existingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return receivedUnitCost != null ? receivedUnitCost.setScale(COST_SCALE, ROUNDING_MODE) : BigDecimal.ZERO;
        }

        if (receivedQuantity == null || receivedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return existingCost != null ? existingCost.setScale(COST_SCALE, ROUNDING_MODE) : BigDecimal.ZERO;
        }

        BigDecimal safeExistingCost = existingCost != null ? existingCost : BigDecimal.ZERO;
        BigDecimal safeReceivedCost = receivedUnitCost != null ? receivedUnitCost : BigDecimal.ZERO;

        BigDecimal existingTotalValue = existingQuantity.multiply(safeExistingCost);
        BigDecimal receivedTotalValue = receivedQuantity.multiply(safeReceivedCost);
        BigDecimal newTotalQuantity = existingQuantity.add(receivedQuantity);

        if (newTotalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return existingTotalValue.add(receivedTotalValue)
                .divide(newTotalQuantity, COST_SCALE, ROUNDING_MODE);
    }
}

