package com.shkgroups.payments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MercadoPagoWebhookRequest(
        @NotNull @Valid Mp mp
) {
    public record Mp(
            String paymentId,
            String topic,
            String token,
            Map<String, Object> headers,
            Map<String, Object> query,
            Map<String, Object> body
    ) {}
}