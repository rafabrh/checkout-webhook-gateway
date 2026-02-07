package com.shkgroups.payments;

import com.shkgroups.common.PlanId;
import com.shkgroups.config.MercadoPagoProperties;
import com.shkgroups.orders.OrderEntity;
import com.shkgroups.orders.OrderRepository;
import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.payments.dto.CheckoutCreateResponse;
import com.shkgroups.payments.mp.MercadoPagoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final MercadoPagoClient mpClient;
    private final MercadoPagoProperties mpProps;

    @Transactional
    public CheckoutCreateResponse createCheckout(CheckoutCreateRequest req) {

        var plan = PlanId.from(req.plan());

        var orderId = UUID.randomUUID().toString();
        var now = OffsetDateTime.now();

        var order = OrderEntity.builder()
                .orderId(orderId)
                .instance(req.instance())
                .remoteJid(req.remoteJid())
                .plan(plan.getId())
                .channel(req.channel())
                .status(OrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderRepository.save(order);

        if (!mpProps.hasAccessToken()) {
            var checkoutUrl = mpProps.getStaticCheckoutUrl();
            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new IllegalStateException("mp_access_token_empty_and_static_checkout_url_empty");
            }
            var msg = "Segue seu link de pagamento (Pix/Cartão): " + checkoutUrl;
            return new CheckoutCreateResponse(orderId, checkoutUrl, msg);
        }

        var notificationUrl = mpProps.resolveNotificationUrl();
        if (notificationUrl == null || notificationUrl.isBlank()) {
            log.error("MP notification_url is missing. Configure app.mercadopago.notification-url OR app.mercadopago.n8n-webhook-url.");
            throw new IllegalStateException("mp_notification_url_is_required");
        }

        log.info("Creating MP preference. orderId={}, plan={}, notificationUrl={}", orderId, plan.getId(), notificationUrl);

        var pref = new MercadoPagoClient.MpPreferenceCreate(
                List.of(new MercadoPagoClient.MpPreferenceItem(
                        plan.getTitle(), 1, plan.getPrice()
                )),
                orderId,
                notificationUrl
        );

        var created = mpClient.createPreference(pref);

        var checkoutUrl = created.initPoint();
        var messageText = "Segue seu link de pagamento (Pix/Cartão): " + checkoutUrl;

        return new CheckoutCreateResponse(orderId, checkoutUrl, messageText);
    }
}
