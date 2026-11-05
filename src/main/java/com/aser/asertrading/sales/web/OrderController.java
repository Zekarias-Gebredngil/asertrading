package com.aser.asertrading.sales.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.identity.service.UserPrincipal;
import com.aser.asertrading.sales.domain.OrderStatus;
import com.aser.asertrading.sales.service.OrderService;
import com.aser.asertrading.sales.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Orders & Checkout", description = "Endpoints for order quote calculation, checkout, and lifecycle management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout/quote")
    @Operation(summary = "Validate cart, check cross-branch stock availability, and compute server-side totals")
    public ApiResponse<CheckoutQuoteResponse> calculateCheckoutQuote(@Valid @RequestBody CheckoutQuoteRequest request) {
        return ApiResponse.success("Quote generated", orderService.calculateCheckoutQuote(request));
    }

    @PostMapping("/orders")
    @Operation(summary = "Create an order from checkout and atomically reserve inventory")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        Long customerId = null;
        String actor = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            customerId = principal.getId();
            actor = principal.getUsername();
        }

        OrderDto order = orderService.createOrder(request, customerId, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/orders")
    @Operation(summary = "List customer or company orders with optional status filter")
    public ApiResponse<PagedResponse<OrderDto>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean isStaff = principal.getAuthorities().stream()
                    .anyMatch(a -> !a.getAuthority().equals("ROLE_CUSTOMER"));

            if (isStaff) {
                return ApiResponse.success(PagedResponse.of(orderService.getOrders(status, pageable)));
            } else {
                return ApiResponse.success(PagedResponse.of(orderService.getCustomerOrders(principal.getEmail(), pageable)));
            }
        }

        return ApiResponse.success(PagedResponse.of(orderService.getOrders(status, pageable)));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get order details by ID")
    public ApiResponse<OrderDto> getOrderById(@PathVariable Long id, Authentication authentication) {
        OrderDto order = orderService.getOrderById(id);

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean isStaff = principal.getAuthorities().stream()
                    .anyMatch(a -> !a.getAuthority().equals("ROLE_CUSTOMER"));

            if (!isStaff) {
                boolean isOwner = (order.customerId() != null && order.customerId().equals(principal.getId()))
                        || (order.customerEmail() != null && order.customerEmail().equalsIgnoreCase(principal.getEmail()));
                if (!isOwner) {
                    throw new AccessDeniedException("Access denied: You do not have permission to view this order.");
                }
            }
        }

        return ApiResponse.success(order);
    }

    @GetMapping("/orders/number/{orderNumber}")
    @Operation(summary = "Get order details by order number")
    public ApiResponse<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        return ApiResponse.success(orderService.getOrderByNumber(orderNumber));
    }

    @PostMapping("/orders/{id}/confirm")
    @Operation(summary = "Confirm order (fulfills reserved stock into sales issue atomically)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'SALES_OFFICER')")
    public ApiResponse<OrderDto> confirmOrder(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success("Order confirmed", orderService.confirmOrder(id, auth.getName()));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel order (releases reserved stock atomically)")
    public ApiResponse<OrderDto> cancelOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Customer cancellation") String reason,
            Authentication auth) {

        String actor = auth != null ? auth.getName() : "ANONYMOUS";
        return ApiResponse.success("Order cancelled", orderService.cancelOrder(id, reason, actor));
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Update order status and optional payment status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'SALES_OFFICER', 'STOREKEEPER')")
    public ApiResponse<OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication auth) {

        return ApiResponse.success("Order status updated", orderService.updateOrderStatus(id, request.status(), request.comment(), request.paymentStatus(), auth.getName()));
    }
}

