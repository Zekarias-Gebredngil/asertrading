package com.aser.asertrading.sales.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.sales.domain.Order;
import com.aser.asertrading.sales.domain.OrderStatus;
import com.aser.asertrading.sales.domain.Payment;
import com.aser.asertrading.sales.domain.PaymentStatus;
import com.aser.asertrading.sales.repository.OrderRepository;
import com.aser.asertrading.sales.repository.PaymentRepository;
import com.aser.asertrading.sales.web.dto.ConfirmPaymentRequest;
import com.aser.asertrading.sales.web.dto.InitiatePaymentRequest;
import com.aser.asertrading.sales.web.dto.PaymentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditService auditService;

    @Transactional
    public PaymentDto initiatePayment(InitiatePaymentRequest request, String actor) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order is not awaiting payment. Current status: " + order.getStatus());
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .paymentMethod(request.paymentMethod())
                .amount(request.amount())
                .currency("ETB")
                .status(PaymentStatus.PENDING)
                .referenceNumber(request.referenceNumber())
                .notes(request.notes())
                .build();

        payment = paymentRepository.save(payment);

        auditService.log(actor, "INITIATE_PAYMENT", "Payment", payment.getId().toString(),
                "Initiated " + payment.getPaymentMethod() + " payment for order " + order.getOrderNumber() + " (" + payment.getAmount() + " ETB)", null);

        return PaymentDto.of(payment, order.getOrderNumber());
    }

    @Transactional
    public PaymentDto confirmPayment(Long paymentId, ConfirmPaymentRequest request, String confirmedBy) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is already finalized. Current status: " + payment.getStatus());
        }

        final Long orderId = payment.getOrderId();
        payment.setStatus(PaymentStatus.SUCCESSFUL);
        payment.setReferenceNumber(request.referenceNumber().trim());
        payment.setConfirmedBy(confirmedBy);
        payment.setConfirmedAt(Instant.now());
        if (request.notes() != null) {
            payment.setNotes(request.notes());
        }
        Payment savedPayment = paymentRepository.save(payment);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Advance order to CONFIRMED and fulfill inventory reservation atomically!
        orderService.confirmOrder(order.getId(), confirmedBy);

        auditService.log(confirmedBy, "CONFIRM_PAYMENT", "Payment", savedPayment.getId().toString(),
                String.format("Confirmed payment %s (Ref: %s) for order %s", savedPayment.getId(), savedPayment.getReferenceNumber(), order.getOrderNumber()), null);

        return PaymentDto.of(savedPayment, order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        return PaymentDto.of(payment, order != null ? order.getOrderNumber() : "UNKNOWN");
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return paymentRepository.findByOrderId(orderId).stream()
                .map(p -> PaymentDto.of(p, order.getOrderNumber()))
                .toList();
    }
}

