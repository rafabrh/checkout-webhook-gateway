package com.shkgroups.provisioning.dto;

import jakarta.validation.constraints.NotBlank;

public record ProvisionPairingRequest(
        @NotBlank String orderId,
        @NotBlank String remoteJid,
        @NotBlank String plan,
        @NotBlank String paymentId
) {}