package com.shkgroups.application.payments.usecase;

import com.shkgroups.config.properties.MercadoPagoProperties;
import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.payments.dto.CheckoutCreateResponse;
import com.shkgroups.ports.payments.OrderStorePort;
import com.shkgroups.ports.payments.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateCheckoutUseCase {

    private final OrderStorePort orderStorePort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final MercadoPagoProperties mpProperties;

    public CheckoutCreateResponse execute(CheckoutCreateRequest request) {
        final String runId = UUID.randomUUID().toString();
        final String orderId = UUID.randomUUID().toString();
        final var plan = request.plan();

        orderStorePort.createCreatedOrder(request, orderId);

        if (!mpProperties.hasAccessToken()) {
            String checkoutUrl = mpProperties.getStaticCheckoutUrl();
            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new IllegalStateException("mp_access_token_empty_and_static_checkout_url_empty");
            }
            return response(orderId, checkoutUrl);
        }

        String notificationUrl = mpProperties.resolveNotificationUrl();
        if (notificationUrl == null || notificationUrl.isBlank()) {
            log.error("mp_notification_url_missing runId={} orderId={}", runId, orderId);
            throw new IllegalStateException("mp_notification_url_is_required");
        }

        log.info("creating_mp_preference runId={} orderId={} plan={} notificationHost={} at={}",
                runId,
                orderId,
                plan.getId(),
                safeHost(notificationUrl),
                OffsetDateTime.now(ZoneOffset.UTC));

        try {
            String checkoutUrl = paymentGatewayPort.createCheckoutPreference(
                    new PaymentGatewayPort.CreatePreferenceCommand(
                            plan.getTitle(),
                            plan.getPrice(),
                            orderId,
                            notificationUrl
                    )
            );
            return response(orderId, checkoutUrl);
        } catch (Exception ex) {
            orderStorePort.markCanceled(orderId);
            log.warn("checkout_preference_failed runId={} orderId={} reason={}", runId, orderId, ex.getMessage());
            throw ex;
        }
    }

    private CheckoutCreateResponse response(String orderId, String checkoutUrl) {
        String messageText = "Segue seu link de pagamento (Pix/Cartão): " + checkoutUrl;
        return new CheckoutCreateResponse(orderId, checkoutUrl, messageText);
    }

    private String safeHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host == null ? "unknown" : host;
        } catch (Exception ex) {
            return "invalid";
        }
    }
}
