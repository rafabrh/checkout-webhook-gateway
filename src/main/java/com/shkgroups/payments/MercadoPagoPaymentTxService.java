package com.shkgroups.payments;

import com.shkgroups.orders.OrderStatus;
import com.shkgroups.payments.domain.MpDecision;
import com.shkgroups.shared.domain.PlanId;
import com.shkgroups.orders.OrderRepository;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import com.shkgroups.payments.mp.MercadoPagoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoPaymentTxService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public MercadoPagoWebhookResponse handle(String paymentId, MercadoPagoClient.MpPayment payment) {

        if (payment == null) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId), "mp_payment_null");
        }

        var orderId = payment.externalReference();
        if (orderId == null || orderId.isBlank()) {
            upsertPayment(paymentId, null, payment);
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId), "no_external_reference");
        }

        var order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            upsertPayment(paymentId, orderId, payment);
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId), "order_not_found");
        }

        if (order.getStatus() == OrderStatus.PROVISIONED) {
            upsertPayment(paymentId, orderId, payment);
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "already_provisioned"
            );
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            upsertPayment(paymentId, orderId, payment);
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "order_canceled"
            );
        }

        var decision = MpStatusEvaluator.decisionFor(payment.status());

        upsertPayment(paymentId, orderId, payment);

        if (decision != MpDecision.PROVISION) {
            return new MercadoPagoWebhookResponse(
                    decision,
                    new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "mp_status:" + payment.status()
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

        var amount = payment.transactionAmount();
        if (amount == null || !amountMatches(plan.getPrice(), amount)) {
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

        return new MercadoPagoWebhookResponse(
                MpDecision.PROVISION,
                new MercadoPagoWebhookResponse.OrderRef(orderId, order.getRemoteJid(), order.getPlan()),
                new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                null
        );
    }

    private void upsertPayment(String paymentId, String orderId, MercadoPagoClient.MpPayment payment) {
        try {
            var existing = paymentRepository.findByPaymentId(paymentId).orElse(null);
            if (existing == null) {
                paymentRepository.save(PaymentsEntity.builder()
                        .paymentId(paymentId)
                        .orderId(orderId)
                        .status(payment.status())
                        .amount(payment.transactionAmount())
                        .build());
            } else {
                existing.setOrderId(orderId);
                existing.setStatus(payment.status());
                existing.setAmount(payment.transactionAmount());
                paymentRepository.save(existing);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Payment {} upsert race; ignoring.", paymentId);
        }
    }

    private static boolean amountMatches(BigDecimal expected, BigDecimal got) {
        if (expected == null || got == null) return false;
        return expected.compareTo(got) == 0;
    }
}
