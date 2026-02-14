package com.shkgroups.payments;

import com.shkgroups.common.PlanId;
import com.shkgroups.config.MercadoPagoProperties;
import com.shkgroups.orders.OrderRepository;
import com.shkgroups.payments.dto.MercadoPagoWebhookRequest;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import com.shkgroups.payments.mp.MercadoPagoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookService {

    private final MercadoPagoClient mpClient;
    private final MercadoPagoProperties mpProps;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public MercadoPagoWebhookResponse process(MercadoPagoWebhookRequest req) {
        var token = (req != null && req.mp() != null) ? req.mp().token() : null;
        var paymentId = (req != null && req.mp() != null) ? req.mp().paymentId() : null;
        return process(token, paymentId);
    }

    public MercadoPagoWebhookResponse process(String token, String paymentId) {

        if (!isValidWebhookToken(token)) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, null, "invalid_webhook_token");
        }

        if (paymentId == null || paymentId.isBlank()) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, null, "payment_id_missing");
        }

        var existingPayment = paymentRepository.findByPaymentId(paymentId);
        if (existingPayment.isPresent()) {
            var p = existingPayment.get();
            var order = orderRepository.findByOrderId(p.getOrderId()).orElse(null);

            if (order != null && order.getStatus() == OrderStatus.PROVISIONED) {
                return new MercadoPagoWebhookResponse(
                        MpDecision.IGNORE,
                        new MercadoPagoWebhookResponse.OrderRef(order.getOrderId(), order.getRemoteJid(), order.getPlan()),
                        new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                        "already_provisioned"
                );
            }

            return new MercadoPagoWebhookResponse(
                    MpDecision.PROVISION,
                    order == null ? null : new MercadoPagoWebhookResponse.OrderRef(order.getOrderId(), order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "already_processed_retry_allowed"
            );
        }

        var payment = mpClient.getPayment(paymentId);

        return processApprovedPaymentInTx(paymentId, payment);
    }

    @Transactional
    protected MercadoPagoWebhookResponse processApprovedPaymentInTx(String paymentId, MercadoPagoClient.MpPayment payment) {

        if (payment == null || payment.externalReference() == null || payment.externalReference().isBlank()) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, new MercadoPagoWebhookResponse.PaymentRef(paymentId), "no_external_reference");
        }

        var orderId = payment.externalReference();
        var order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, new MercadoPagoWebhookResponse.PaymentRef(paymentId), "order_not_found");
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "order_canceled"
            );
        }

        if (!"approved".equalsIgnoreCase(payment.status())) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "status_not_approved:" + payment.status()
            );
        }

        final PlanId plan;
        try {
            plan = PlanId.from(order.getPlan());
        } catch (Exception e) {
            log.warn("Invalid plan in order {}: {}", orderId, order.getPlan());
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "invalid_plan"
            );
        }

        if (payment.transactionAmount() == null || plan.getPrice().compareTo(payment.transactionAmount()) != 0) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "amount_mismatch"
            );
        }

        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.PAID);
            order.touchUpdate();
            orderRepository.save(order);
        }

        try {
            paymentRepository.save(PaymentsEntity.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .status(payment.status())
                    .amount(payment.transactionAmount())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Payment {} already inserted (race).", paymentId);
        }

        return new MercadoPagoWebhookResponse(
                MpDecision.PROVISION,
                new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                null
        );
    }

    private boolean isValidWebhookToken(String got) {
        var expected = mpProps.getWebhookToken();
        if (expected == null || expected.isBlank()) {
            log.warn("mp webhook-token is not configured (app.mercadopago.webhook-token).");
            return false;
        }
        if (got == null || got.isBlank()) return false;

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                got.getBytes(StandardCharsets.UTF_8)
        );
    }
}
