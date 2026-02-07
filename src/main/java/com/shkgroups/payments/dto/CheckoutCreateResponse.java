package com.shkgroups.payments.dto;


public record CheckoutCreateResponse(
        String orderId,
        String checkoutUrl,
        String messageText
) {}