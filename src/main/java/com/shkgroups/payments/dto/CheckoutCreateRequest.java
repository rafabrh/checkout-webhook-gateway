package com.shkgroups.payments.dto;

import com.shkgroups.shared.domain.PlanId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutCreateRequest(
        @NotBlank(message = "instance_required")
        String instance,

        @NotBlank(message = "remote_jid_required")
        String remoteJid,

        @NotNull(message = "plan_required")
        PlanId plan,

        @NotBlank(message = "channel_required")
        String channel,

        @Valid
        Customer customer
) {
    public record Customer(
            @NotBlank(message = "customer_name_required")
            String name
    ) {}
}
