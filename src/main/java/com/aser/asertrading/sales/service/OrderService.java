package com.aser.asertrading.sales.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.InsufficientStockException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.service.SecurityBranchService;
import com.aser.asertrading.identity.service.UserPrincipal;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.service.StockReservationService;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.sales.domain.Customer;
import com.aser.asertrading.sales.domain.DeliveryType;
import com.aser.asertrading.sales.domain.Order;
import com.aser.asertrading.sales.domain.OrderItem;
import com.aser.asertrading.sales.domain.OrderStatus;
import com.aser.asertrading.sales.domain.OrderStatusHistory;
import com.aser.asertrading.sales.domain.Payment;
import com.aser.asertrading.sales.domain.PaymentStatus;
import com.aser.asertrading.sales.repository.CustomerRepository;
import com.aser.asertrading.sales.repository.OrderRepository;
import com.aser.asertrading.sales.repository.PaymentRepository;
import com.aser.asertrading.sales.web.dto.CheckoutQuoteRequest;
import com.aser.asertrading.sales.web.dto.CheckoutQuoteResponse;
import com.aser.asertrading.sales.web.dto.CreateOrderRequest;
import com.aser.asertrading.sales.web.dto.OrderDto;
import com.aser.asertrading.sales.web.dto.OrderItemDto;
import com.aser.asertrading.sales.web.dto.OrderStatusHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockReservationService stockReservationService;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;
    private final SecurityBranchService securityBranchService;

    public static final BigDecimal VAT_RATE = BigDecimal.valueOf(0.15); // 15% VAT

    @Transactional(readOnly = true)
    public CheckoutQuoteResponse calculateCheckoutQuote(CheckoutQuoteRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean overallStockAvailable = true;
        List<CheckoutQuoteResponse.QuoteItemDetail> itemDetails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (CheckoutQuoteRequest.CartItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.productId()));

            if (!product.isPublished() || product.getStatus() != ProductStatus.ACTIVE) {
                warnings.add("Product '" + product.getName() + "' is currently inactive or unpublished");
                overallStockAvailable = false;
                continue;
            }

            BigDecimal availableStock = stockBalanceRepository.getTotalOnHandByProduct(product.getId());
            BigDecimal reservedStock = stockBalanceRepository.getTotalReservedByProduct(product.getId());
            BigDecimal effectiveAvailable = availableStock != null && reservedStock != null
                    ? availableStock.subtract(reservedStock)
                    : BigDecimal.ZERO;

            boolean itemAvailable = effectiveAvailable.compareTo(itemReq.quantity()) >= 0;
            if (!itemAvailable) {
                overallStockAvailable = false;
                warnings.add(String.format("Insufficient stock for %s. Requested: %s, Available across branches: %s",
                        product.getName(), itemReq.quantity(), effectiveAvailable));
            }

            BigDecimal lineTotal = itemReq.quantity().multiply(product.getSellingPrice()).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            itemDetails.add(new CheckoutQuoteResponse.QuoteItemDetail(
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    itemReq.quantity(),
                    product.getSellingPrice(),
                    lineTotal,
                    itemAvailable
            ));
        }

        BigDecimal deliveryFee = request.deliveryType() == DeliveryType.DELIVERY
                ? BigDecimal.valueOf(150.00) // Configurable flat delivery fee baseline
                : BigDecimal.ZERO;

        BigDecimal taxAmount = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(deliveryFee).add(taxAmount);

        return new CheckoutQuoteResponse(
                subtotal,
                deliveryFee,
                BigDecimal.ZERO,
                taxAmount,
                totalAmount,
                "ETB",
                overallStockAvailable,
                itemDetails,
                warnings
        );
    }

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, Long customerId, String actor) {
        // Resolve or create customer record
        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        }
        if (customer == null && request.customerEmail() != null) {
            customer = customerRepository.findByEmail(request.customerEmail()).orElseGet(() ->
                    customerRepository.save(Customer.builder()
                            .fullName(request.customerName().trim())
                            .email(request.customerEmail().trim().toLowerCase())
                            .phone(request.customerPhone())
                            .address(request.deliveryAddress())
                            .build())
            );
        }

        // Determine fulfillment warehouse
        Long warehouseId = request.fulfillmentWarehouseId();
        if (warehouseId == null && request.collectionBranchId() != null) {
            warehouseId = warehouseRepository.findByBranchIdAndIsDefaultTrue(request.collectionBranchId())
                    .or(() -> warehouseRepository.findByBranchId(request.collectionBranchId()).stream().findFirst())
                    .map(Warehouse::getId)
                    .orElse(null);
        }
        if (warehouseId == null) {
            if (request.items() != null && !request.items().isEmpty()) {
                Long firstProductId = request.items().get(0).productId();
                warehouseId = stockBalanceRepository.findByProductId(firstProductId).stream()
                        .filter(b -> b.getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0)
                        .map(StockBalance::getWarehouseId)
                        .findFirst()
                        .orElse(null);
            }
            if (warehouseId == null) {
                warehouseId = warehouseRepository.findByIsActiveTrue().stream()
                        .findFirst()
                        .map(Warehouse::getId)
                        .orElseThrow(() -> new BadRequestException("No active warehouse available for order fulfillment"));
            }
        }

        String orderNumber = "ORD-" + System.currentTimeMillis() % 10000000;

        Long branchId = request.collectionBranchId();
        if (branchId == null && warehouseId != null) {
            branchId = warehouseRepository.findById(warehouseId)
                    .filter(w -> w.getBranch() != null)
                    .map(w -> w.getBranch().getId())
                    .orElse(null);
        }

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(customer != null ? customer.getId() : null)
                .customerName(request.customerName().trim())
                .customerEmail(request.customerEmail().trim().toLowerCase())
                .customerPhone(request.customerPhone())
                .deliveryType(request.deliveryType())
                .deliveryAddress(request.deliveryAddress())
                .collectionBranchId(branchId)
                .fulfillmentWarehouseId(warehouseId)
                .status(OrderStatus.PENDING_PAYMENT)
                .notes(request.notes())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.productId()));

            if (!product.isPublished() || product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Product " + product.getName() + " is inactive or unavailable for order");
            }

            BigDecimal lineTotal = itemReq.quantity().multiply(product.getSellingPrice()).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .quantity(itemReq.quantity())
                    .unitPrice(product.getSellingPrice())
                    .totalPrice(lineTotal)
                    .build();

            order.getItems().add(item);
        }

        BigDecimal deliveryFee = request.deliveryType() == DeliveryType.DELIVERY ? BigDecimal.valueOf(150.00) : BigDecimal.ZERO;
        BigDecimal taxAmount = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(deliveryFee).add(taxAmount);

        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING_PAYMENT)
                .actor(actor != null ? actor : "CUSTOMER")
                .comment("Order placed, awaiting payment confirmation")
                .build();
        order.getStatusHistory().add(history);

        order = orderRepository.save(order);

        // Reserve inventory atomically across the items
        for (OrderItem item : order.getItems()) {
            try {
                stockReservationService.reserveStock(order.getId(), warehouseId, item.getProductId(), item.getQuantity(), 24);
            } catch (InsufficientStockException ex) {
                // If reservation fails, rollback will trigger from transaction
                throw new InsufficientStockException("Cannot place order #" + order.getOrderNumber() + ": " + ex.getMessage());
            }
        }

        // Create pending payment record if payment method requested
        if (request.paymentMethod() != null) {
            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .paymentMethod(request.paymentMethod())
                    .amount(totalAmount)
                    .currency("ETB")
                    .status(PaymentStatus.PENDING)
                    .referenceNumber(request.paymentReference())
                    .notes("Pending payment for order #" + order.getOrderNumber())
                    .build();
            paymentRepository.save(payment);
        }

        auditService.log(actor != null ? actor : order.getCustomerEmail(), "CREATE_ORDER", "Order", order.getId().toString(),
                "Placed order " + order.getOrderNumber() + " totaling " + order.getTotalAmount() + " ETB", null);

        return mapToDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus, String comment, String actor) {
        return updateOrderStatus(orderId, newStatus, comment, null, actor);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus, String comment, String paymentStatusStr, String actor) {
        Order order = getOrderEntity(orderId);

        Optional<UserPrincipal> principalOpt = securityBranchService.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            UserPrincipal principal = principalOpt.get();
            if (principal.getRole() == Role.CUSTOMER) {
                boolean isOwner = (order.getCustomerId() != null && order.getCustomerId().equals(principal.getId()))
                        || (order.getCustomerEmail() != null && order.getCustomerEmail().equalsIgnoreCase(principal.getEmail()));
                if (!isOwner) {
                    throw new AccessDeniedException("Access denied: You do not have permission to modify this order.");
                }
                if (newStatus != OrderStatus.CANCELLED) {
                    throw new AccessDeniedException("Access denied: Customers are only permitted to cancel pending orders.");
                }
                if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                    throw new BadRequestException("Order cannot be cancelled because it is currently " + order.getStatus() + ".");
                }
            } else if (!securityBranchService.canAccessOrder(order.getCollectionBranchId(), order.getFulfillmentWarehouseId())) {
                throw new AccessDeniedException("Access denied: You do not have permission to manage orders for another branch.");
            }
        } else if (!securityBranchService.canAccessOrder(order.getCollectionBranchId(), order.getFulfillmentWarehouseId())) {
            throw new AccessDeniedException("Access denied: You do not have permission to manage orders for another branch.");
        }

        OrderStatus currentStatus = order.getStatus();

        // Validate allowed transitions
        if (currentStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered orders cannot be modified. Process returns or refunds via return workflow.");
        }
        if (currentStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled orders cannot be reopened.");
        }

        // If transitioning to CANCELLED: release reservations
        if (newStatus == OrderStatus.CANCELLED) {
            stockReservationService.releaseReservationForOrder(order.getId(), actor);
        }

        // If transitioning from PENDING_PAYMENT to any progress status: fulfill reservations into stock issues atomically!
        if (currentStatus == OrderStatus.PENDING_PAYMENT && newStatus != OrderStatus.PENDING_PAYMENT && newStatus != OrderStatus.CANCELLED) {
            stockReservationService.fulfillReservationForOrder(order.getId(), actor);
        }

        order.setStatus(newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .actor(actor)
                .comment(comment != null ? comment : "Status updated to " + newStatus)
                .build();
        order.getStatusHistory().add(history);

        // Update payment status if explicitly requested or if moving to paid / confirmed lifecycle
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        if (!payments.isEmpty()) {
            Payment p = payments.get(0);
            if (paymentStatusStr != null && !paymentStatusStr.isBlank()) {
                String norm = paymentStatusStr.trim().toUpperCase();
                if (norm.equals("PAID") || norm.equals("VERIFIED") || norm.equals("SUCCESSFUL")) {
                    p.setStatus(PaymentStatus.SUCCESSFUL);
                    p.setConfirmedBy(actor);
                    p.setConfirmedAt(Instant.now());
                } else if (norm.equals("PENDING")) {
                    p.setStatus(PaymentStatus.PENDING);
                } else if (norm.equals("FAILED")) {
                    p.setStatus(PaymentStatus.FAILED);
                } else if (norm.equals("CANCELLED")) {
                    p.setStatus(PaymentStatus.CANCELLED);
                }
                paymentRepository.save(p);
            } else if (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.READY_FOR_DISPATCH || newStatus == OrderStatus.DELIVERED) {
                if (p.getStatus() == PaymentStatus.PENDING) {
                    p.setStatus(PaymentStatus.SUCCESSFUL);
                    p.setConfirmedBy(actor);
                    p.setConfirmedAt(Instant.now());
                    paymentRepository.save(p);
                }
            } else if (newStatus == OrderStatus.CANCELLED) {
                p.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(p);
            }
        }

        order = orderRepository.save(order);

        auditService.log(actor, "UPDATE_ORDER_STATUS", "Order", order.getId().toString(),
                String.format("Updated order %s status: %s -> %s (%s)", order.getOrderNumber(), currentStatus, newStatus, comment), null);

        return mapToDto(order);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId, String reason, String actor) {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED, "Cancellation reason: " + reason, actor);
    }

    @Transactional
    public OrderDto confirmOrder(Long orderId, String actor) {
        return updateOrderStatus(orderId, OrderStatus.CONFIRMED, "Order confirmed by " + actor, actor);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrders(OrderStatus status, Pageable pageable) {
        return getOrders(status, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrders(OrderStatus status, Long requestedBranchId, Pageable pageable) {
        Page<Order> orders;

        if (securityBranchService.hasGlobalBranchAccess()) {
            if (requestedBranchId != null) {
                orders = orderRepository.findByBranchAndOptionalStatus(requestedBranchId, status, pageable);
            } else if (status != null) {
                orders = orderRepository.findByStatus(status, pageable);
            } else {
                orders = orderRepository.findAll(pageable);
            }
        } else {
            Long callerBranchId = securityBranchService.getCallerBranchId();
            if (callerBranchId == null) {
                orders = Page.empty(pageable);
            } else {
                orders = orderRepository.findByBranchAndOptionalStatus(callerBranchId, status, pageable);
            }
        }

        return orders.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getCustomerOrders(String email, Pageable pageable) {
        return orderRepository.findByCustomerEmail(email, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        Order order = getOrderEntity(id);

        Optional<UserPrincipal> principalOpt = securityBranchService.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            UserPrincipal principal = principalOpt.get();
            if (principal.getRole() == Role.CUSTOMER) {
                boolean isOwner = (order.getCustomerId() != null && order.getCustomerId().equals(principal.getId()))
                        || (order.getCustomerEmail() != null && order.getCustomerEmail().equalsIgnoreCase(principal.getEmail()));
                if (!isOwner) {
                    throw new AccessDeniedException("Access denied: You do not have permission to view this order.");
                }
            } else if (!securityBranchService.canAccessOrder(order.getCollectionBranchId(), order.getFulfillmentWarehouseId())) {
                throw new AccessDeniedException("Access denied: You do not have permission to view orders belonging to another branch.");
            }
        } else if (!securityBranchService.canAccessOrder(order.getCollectionBranchId(), order.getFulfillmentWarehouseId())) {
            throw new AccessDeniedException("Access denied: You do not have permission to view orders belonging to another branch.");
        }

        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
        return mapToDto(order);
    }

    private Order getOrderEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderDto mapToDto(Order order) {
        String branchName = "N/A";
        if (order.getCollectionBranchId() != null) {
            branchName = branchRepository.findById(order.getCollectionBranchId())
                    .map(Branch::getName)
                    .orElse("Unknown");
        }

        String warehouseName = "N/A";
        if (order.getFulfillmentWarehouseId() != null) {
            warehouseName = warehouseRepository.findById(order.getFulfillmentWarehouseId())
                    .map(Warehouse::getName)
                    .orElse("Unknown");
        }

        Payment payment = paymentRepository.findByOrderId(order.getId()).stream().findFirst().orElse(null);
        String paymentMethod = payment != null && payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "CASH";
        String paymentStatus = payment != null && payment.getStatus() != null ? payment.getStatus().name() : "PENDING";
        String paymentRef = payment != null ? payment.getReferenceNumber() : null;

        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(OrderItemDto::fromEntity)
                .toList();

        List<OrderStatusHistoryDto> historyDtos = order.getStatusHistory().stream()
                .map(OrderStatusHistoryDto::fromEntity)
                .toList();

        return OrderDto.of(order, branchName, warehouseName, paymentMethod, paymentStatus, paymentRef, itemDtos, historyDtos);
    }
}

