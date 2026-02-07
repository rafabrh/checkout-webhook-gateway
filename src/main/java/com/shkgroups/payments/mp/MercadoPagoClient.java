package com.shkgroups.payments.mp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shkgroups.config.MercadoPagoProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MercadoPagoClient {

    private final WebClient mpWebClient;
    private final MercadoPagoProperties props;

    public MercadoPagoClient(
            @Qualifier("mpWebClient") WebClient mpWebClient,
            MercadoPagoProperties props
    ) {
        this.mpWebClient = mpWebClient;
        this.props = props;
    }

    public MpPayment getPayment(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) return null;

        var token = props.getAccessToken();
        if (token == null || token.isBlank()) throw new IllegalStateException("mp_access_token_empty");

        return mpWebClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .headers(h -> h.setBearerAuth(token.trim()))
                .retrieve()
                .bodyToMono(MpPayment.class)
                .block();
    }

    public MpPreferenceCreated createPreference(MpPreferenceCreate req) {
        var token = props.getAccessToken();
        if (token == null || token.isBlank()) throw new IllegalStateException("mp_access_token_empty");

        return mpWebClient.post()
                .uri("/checkout/preferences")
                .headers(h -> h.setBearerAuth(token.trim()))
                .bodyValue(req)
                .retrieve()
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class)
                                .map(body -> new IllegalStateException("MP " + resp.statusCode() + " -> " + body))
                )
                .bodyToMono(MpPreferenceCreated.class)
                .block();
    }

    public record MpPayment(
            String id,
            String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("currency_id") String currencyId,
            @JsonProperty("external_reference") String externalReference
    ) {}

    public record MpPreferenceItem(
            String title,
            Integer quantity,
            @JsonProperty("unit_price") BigDecimal unitPrice
    ) {}

    public record MpPreferenceCreate(
            List<MpPreferenceItem> items,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("notification_url") String notificationUrl
    ) {}

    public record MpPreferenceCreated(
            String id,
            @JsonProperty("init_point") String initPoint
    ) {}
}
