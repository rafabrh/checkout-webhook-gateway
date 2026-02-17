package com.shkgroups.payments;

import com.shkgroups.config.properties.MercadoPagoProperties;
import com.shkgroups.payments.domain.MpDecision;
import com.shkgroups.payments.dto.MercadoPagoWebhookRequest;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import com.shkgroups.payments.mp.MercadoPagoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookService {

    private final MercadoPagoClient mpClient;
    private final MercadoPagoProperties mpProps;
    private final MercadoPagoPaymentTxService txService;

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

        final MercadoPagoClient.MpPayment payment;
        try {
            payment = mpClient.getPayment(paymentId);
        } catch (Exception e) {
            log.warn("Failed to fetch payment {} from MP: {}", paymentId, e.getMessage());
            return new MercadoPagoWebhookResponse(
                    MpDecision.IGNORE,
                    null,
                    new MercadoPagoWebhookResponse.PaymentRef(paymentId),
                    "mp_fetch_failed"
            );
        }

        return txService.handle(paymentId, payment);
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
