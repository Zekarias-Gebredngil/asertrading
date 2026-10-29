package com.aser.asertrading.sales.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.sales.service.PaymentService;
import com.aser.asertrading.sales.web.dto.ConfirmPaymentRequest;
import com.aser.asertrading.sales.web.dto.InitiatePaymentRequest;
import com.aser.asertrading.sales.web.dto.PaymentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for manual transfer, cash and payment confirmation")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment attempt for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request, Authentication auth) {
        String actor = auth != null ? auth.getName() : "CUSTOMER";
        PaymentDto payment = paymentService.initiatePayment(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payment initiated", payment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment status by payment ID")
    public ApiResponse<PaymentDto> getPaymentById(@PathVariable Long id) {
        return ApiResponse.success(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payments for an order")
    public ApiResponse<List<PaymentDto>> getPaymentsByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(paymentService.getPaymentsByOrder(orderId));
    }

    @PostMapping("/{id}/confirm-manual")
    @Operation(summary = "Confirm manual bank transfer or cash in hand payment (advances order to CONFIRMED and issues reserved stock)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ACCOUNTANT_AUDITOR', 'SALES_OFFICER', 'COMPANY_MANAGER')")
    public ApiResponse<PaymentDto> confirmPayment(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmPaymentRequest request,
            Authentication auth) {

        PaymentDto payment = paymentService.confirmPayment(id, request, auth.getName());
        return ApiResponse.success("Payment confirmed and order marked CONFIRMED", payment);
    }
}

