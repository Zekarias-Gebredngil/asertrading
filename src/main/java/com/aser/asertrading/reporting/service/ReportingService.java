package com.aser.asertrading.reporting.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.reporting.dto.*;
import com.aser.asertrading.sales.domain.DeliveryType;
import com.aser.asertrading.sales.domain.Order;
import com.aser.asertrading.sales.domain.OrderStatus;
import com.aser.asertrading.sales.repository.OrderRepository;
import com.aser.asertrading.transfer.domain.StockTransfer;
import com.aser.asertrading.transfer.domain.TransferStatus;
import com.aser.asertrading.transfer.repository.StockTransferRepository;
import com.aser.asertrading.transfer.repository.TransferDiscrepancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final StockBalanceRepository stockBalanceRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final StockTransferRepository transferRepository;
    private final TransferDiscrepancyRepository discrepancyRepository;

    @Transactional(readOnly = true)
    public StockSummaryReportDto getStockSummary() {
        List<StockBalance> balances = stockBalanceRepository.findAll();
        List<Branch> branches = branchRepository.findAll();
        List<Warehouse> warehouses = warehouseRepository.findAll();

        Map<Long, List<Warehouse>> branchWarehouses = warehouses.stream()
                .collect(Collectors.groupingBy(w -> w.getBranch().getId()));

        Map<Long, List<StockBalance>> warehouseBalances = balances.stream()
                .collect(Collectors.groupingBy(StockBalance::getWarehouseId));

        BigDecimal totalOnHand = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;

        List<StockSummaryReportDto.BranchStockSummary> branchSummaries = new ArrayList<>();

        for (Branch b : branches) {
            List<Warehouse> whs = branchWarehouses.getOrDefault(b.getId(), List.of());
            BigDecimal bOnHand = BigDecimal.ZERO;
            BigDecimal bReserved = BigDecimal.ZERO;
            BigDecimal bAvailable = BigDecimal.ZERO;

            for (Warehouse w : whs) {
                List<StockBalance> whBalances = warehouseBalances.getOrDefault(w.getId(), List.of());
                for (StockBalance sb : whBalances) {
                    bOnHand = bOnHand.add(sb.getOnHandQuantity());
                    bReserved = bReserved.add(sb.getReservedQuantity());
                    bAvailable = bAvailable.add(sb.getAvailableQuantity());
                }
            }

            totalOnHand = totalOnHand.add(bOnHand);
            totalReserved = totalReserved.add(bReserved);
            totalAvailable = totalAvailable.add(bAvailable);

            branchSummaries.add(new StockSummaryReportDto.BranchStockSummary(
                    b.getId(),
                    b.getName(),
                    b.getCode(),
                    whs.size(),
                    bOnHand,
                    bReserved,
                    bAvailable
            ));
        }

        long distinctProducts = balances.stream().map(StockBalance::getProductId).distinct().count();

        return new StockSummaryReportDto(
                (int) distinctProducts,
                totalOnHand,
                totalReserved,
                totalAvailable,
                branchSummaries
        );
    }

    @Transactional(readOnly = true)
    public StockValuationReportDto getStockValuation() {
        List<StockBalance> balances = stockBalanceRepository.findAll();
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        BigDecimal totalValuation = BigDecimal.ZERO;
        List<StockValuationReportDto.ProductValuationItem> items = new ArrayList<>();

        for (StockBalance b : balances) {
            Product p = productMap.get(b.getProductId());
            Warehouse w = warehouseMap.get(b.getWarehouseId());

            BigDecimal onHand = b.getOnHandQuantity();
            BigDecimal unitCost = b.getWeightedAverageCost();
            BigDecimal valuation = (onHand != null && unitCost != null)
                    ? onHand.multiply(unitCost).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            totalValuation = totalValuation.add(valuation);

            items.add(new StockValuationReportDto.ProductValuationItem(
                    b.getProductId(),
                    p != null ? p.getSku() : "UNKNOWN",
                    p != null ? p.getName() : "UNKNOWN",
                    p != null ? p.getCategory().getName() : "UNKNOWN",
                    w != null ? w.getName() : "UNKNOWN",
                    onHand,
                    unitCost,
                    valuation
            ));
        }

        return new StockValuationReportDto(totalValuation, items.size(), items);
    }

    @Transactional(readOnly = true)
    public SalesReportDto getSalesReport() {
        BigDecimal totalRevenue = orderRepository.sumTotalAmountExcludingStatus(OrderStatus.CANCELLED);
        long validOrderCount = orderRepository.countExcludingStatus(OrderStatus.CANCELLED);
        long totalOrders = orderRepository.count();

        BigDecimal avgOrderValue = validOrderCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(validOrderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : orderRepository.countOrdersByStatusGroup()) {
            if (row[0] != null && row[1] != null) {
                statusCounts.put(((OrderStatus) row[0]).name(), (Long) row[1]);
            }
        }

        long deliveryCount = orderRepository.countByDeliveryType(DeliveryType.DELIVERY);
        long collectionCount = orderRepository.countByDeliveryType(DeliveryType.COLLECTION);

        return new SalesReportDto(
                (int) totalOrders,
                totalRevenue,
                avgOrderValue,
                statusCounts,
                deliveryCount,
                collectionCount
        );
    }

    @Transactional(readOnly = true)
    public TransferReportDto getTransferReport() {
        long totalTransfers = transferRepository.count();
        long inTransit = transferRepository.countByStatus(TransferStatus.IN_TRANSIT);
        long received = transferRepository.countByStatus(TransferStatus.RECEIVED);
        long discrepancies = discrepancyRepository.count();

        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : transferRepository.countTransfersByStatusGroup()) {
            if (row[0] != null && row[1] != null) {
                statusCounts.put(((TransferStatus) row[0]).name(), (Long) row[1]);
            }
        }

        return new TransferReportDto(
                (int) totalTransfers,
                inTransit,
                received,
                discrepancies,
                statusCounts
        );
    }
}

