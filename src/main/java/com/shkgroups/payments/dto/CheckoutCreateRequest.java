package com.shkgroups.payments.dto;


import jakarta.validation.constraints.NotBlank;

public record CheckoutCreateRequest(
        @NotBlank String instance,
        @NotBlank String remoteJid,
        @NotBlank String plan,
        @NotBlank String channel,
        Customer customer
) {
    public record Customer(String name) {}
}