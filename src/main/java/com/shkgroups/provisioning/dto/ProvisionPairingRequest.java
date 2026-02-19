package com.shkgroups.provisioning.dto;

import com.shkgroups.shared.domain.PlanId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProvisionPairingRequest(
        @NotBlank String orderId,
        @NotBlank String remoteJid,
        @NotNull  PlanId plan,
        @NotBlank String paymentId
) {}