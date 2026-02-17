package com.shkgroups.payments.dto;


import com.shkgroups.payments.domain.MpDecision;

public record MercadoPagoWebhookResponse(
        MpDecision decision,
        OrderRef order,
        PaymentRef payment,
        String reason
) {
    public record OrderRef(String orderId, String remoteJid, String plan) {}
    public record PaymentRef(String id) {}
}