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
        return getStockSummary(null);
    }

    @Transactional(readOnly = true)
    public StockSummaryReportDto getStockSummary(Long branchId) {
        List<Branch> branches = branchId != null
                ? branchRepository.findById(branchId).map(List::of).orElse(List.of())
                : branchRepository.findAll();

        List<Warehouse> warehouses = branchId != null
                ? warehouseRepository.findByBranchId(branchId)
                : warehouseRepository.findAll();

        Set<Long> allowedWarehouseIds = warehouses.stream().map(Warehouse::getId).collect(Collectors.toSet());

        List<StockBalance> balances = stockBalanceRepository.findAll().stream()
                .filter(b -> allowedWarehouseIds.contains(b.getWarehouseId()))
                .toList();

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
        return getStockValuation(null);
    }

    @Transactional(readOnly = true)
    public StockValuationReportDto getStockValuation(Long branchId) {
        List<Warehouse> warehouses = branchId != null
                ? warehouseRepository.findByBranchId(branchId)
                : warehouseRepository.findAll();
        Set<Long> allowedWarehouseIds = warehouses.stream().map(Warehouse::getId).collect(Collectors.toSet());

        List<StockBalance> balances = stockBalanceRepository.findAll().stream()
                .filter(b -> allowedWarehouseIds.contains(b.getWarehouseId()))
                .toList();

        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Warehouse> warehouseMap = warehouses.stream()
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
        return getSalesReport(null);
    }

    @Transactional(readOnly = true)
    public SalesReportDto getSalesReport(Long branchId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .filter(o -> {
                    if (branchId == null) return true;
                    if (o.getCollectionBranchId() != null && o.getCollectionBranchId().equals(branchId)) return true;
                    if (o.getFulfillmentWarehouseId() != null) {
                        Warehouse w = warehouseRepository.findById(o.getFulfillmentWarehouseId()).orElse(null);
                        return w != null && w.getBranch() != null && branchId.equals(w.getBranch().getId());
                    }
                    return false;
                })
                .toList();

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int validOrderCount = orders.size();
        BigDecimal avgOrderValue = validOrderCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(validOrderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        long deliveryCount = orders.stream().filter(o -> o.getDeliveryType() == DeliveryType.DELIVERY).count();
        long collectionCount = orders.stream().filter(o -> o.getDeliveryType() == DeliveryType.COLLECTION).count();

        return new SalesReportDto(
                validOrderCount,
                totalRevenue,
                avgOrderValue,
                statusCounts,
                deliveryCount,
                collectionCount
        );
    }

    @Transactional(readOnly = true)
    public TransferReportDto getTransferReport() {
        return getTransferReport(null);
    }

    @Transactional(readOnly = true)
    public TransferReportDto getTransferReport(Long branchId) {
        List<StockTransfer> transfers;
        if (branchId != null) {
            Set<Long> branchWarehouseIds = warehouseRepository.findByBranchId(branchId).stream()
                    .map(Warehouse::getId)
                    .collect(Collectors.toSet());
            transfers = transferRepository.findAll().stream()
                    .filter(t -> branchWarehouseIds.contains(t.getSourceWarehouseId()) || branchWarehouseIds.contains(t.getDestinationWarehouseId()))
                    .toList();
        } else {
            transfers = transferRepository.findAll();
        }

        long inTransit = transfers.stream().filter(t -> t.getStatus() == TransferStatus.IN_TRANSIT).count();
        long received = transfers.stream().filter(t -> t.getStatus() == TransferStatus.RECEIVED).count();
        long discrepancies = discrepancyRepository.count();

        Map<String, Long> statusCounts = transfers.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        return new TransferReportDto(
                transfers.size(),
                inTransit,
                received,
                discrepancies,
                statusCounts
        );
    }
}
