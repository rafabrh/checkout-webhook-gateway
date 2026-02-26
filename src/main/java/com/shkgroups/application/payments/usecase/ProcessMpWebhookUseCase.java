package com.shkgroups.application.payments.usecase;

import com.shkgroups.config.properties.MercadoPagoProperties;
import com.shkgroups.payments.domain.MpDecision;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import com.shkgroups.ports.payments.OrderStorePort;
import com.shkgroups.ports.payments.PaymentGatewayPort;
import com.shkgroups.ports.payments.PaymentStorePort;
import com.shkgroups.shared.domain.PlanId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessMpWebhookUseCase {

    private final MercadoPagoProperties mpProperties;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentStorePort paymentStorePort;
    private final OrderStorePort orderStorePort;

    public MercadoPagoWebhookResponse execute(String token, String paymentId) {
        final String runId = UUID.randomUUID().toString();

        if (!isValidWebhookToken(token)) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, null, "invalid_webhook_token");
        }

        if (paymentId == null || paymentId.isBlank()) {
            return new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, null, "payment_id_missing");
        }

        final PaymentGatewayPort.MpPaymentData payment;
        try {
            payment = paymentGatewayPort.getPayment(paymentId);
        } catch (Exception ex) {
            log.warn("mp_fetch_failed runId={} paymentId={} reason={}", runId, paymentId, ex.getMessage());
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "mp_fetch_failed"
            );
        }

        if (payment == null) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "mp_payment_null"
            );
        }

        String orderId = payment.externalReference();
        if (orderId == null || orderId.isBlank()) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "no_external_reference"
            );
        }

        paymentStorePort.upsert(paymentId, orderId, payment.status(), payment.transactionAmount());

        var maybeOrder = orderStorePort.findByOrderId(orderId);
        if (maybeOrder.isEmpty()) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "order_not_found"
            );
        }

        var order = maybeOrder.get();

        if ("PROVISIONED".equals(order.status())) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    orderRef(orderId, order.remoteJid(), order.plan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "already_provisioned"
            );
        }

        if ("CANCELED".equals(order.status())) {
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    orderRef(orderId, order.remoteJid(), order.plan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "order_canceled"
            );
        }

        MpDecision decision = decisionFor(payment.status());
        if (decision != MpDecision.PROVISION) {
            return new MercadoPagoWebhookResponse(
                    decision,
                    orderRef(orderId, order.remoteJid(), order.plan()),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "mp_status:" + payment.status()
            );
        }

        PlanId plan = order.plan();
        if (plan == null) {
            log.warn("plan_null runId={} orderId={} paymentId={}", runId, orderId, paymentId);
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    orderRef(orderId, order.remoteJid(), null),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "plan_null"
            );
        }

        if (!amountMatches(plan.getPrice(), payment.transactionAmount())) {
            log.warn("amount_mismatch runId={} orderId={} paymentId={} expected={} got={}",
                    runId, orderId, paymentId, plan.getPrice(), payment.transactionAmount());
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    orderRef(orderId, order.remoteJid(), plan),
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "amount_mismatch"
            );
        }

        orderStorePort.markPaidIfCreated(orderId);

        return new MercadoPagoWebhookResponse(
                MpDecision.PROVISION,
                orderRef(orderId, order.remoteJid(), plan),
                new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                null
        );
    }

    private boolean isValidWebhookToken(String got) {
        String expected = mpProperties.getWebhookToken();
        if (expected == null || expected.isBlank()) {
            log.warn("mp_webhook_token_missing");
            return false;
        }
        if (got == null || got.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                got.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MercadoPagoWebhookResponse.OrderRef orderRef(String orderId, String remoteJid, PlanId plan) {
        String planId = plan == null ? null : plan.getId();
        return new MercadoPagoWebhookResponse.OrderRef(orderId, remoteJid, planId);
    }

    private boolean amountMatches(BigDecimal expected, BigDecimal got) {
        if (expected == null || got == null) {
            return false;
        }
        return expected.compareTo(got) == 0;
    }

    private MpDecision decisionFor(String mpStatus) {
        if (mpStatus == null || mpStatus.isBlank()) {
            return MpDecision.IGNORE;
        }

        return switch (mpStatus.toLowerCase()) {
            case "approved" -> MpDecision.PROVISION;
            case "pending", "in_process", "authorized" -> MpDecision.WAIT_PAYMENT;
            case "rejected", "cancelled", "refunded", "charged_back" -> MpDecision.DENY;
            default -> MpDecision.IGNORE;
        };
    }
}
