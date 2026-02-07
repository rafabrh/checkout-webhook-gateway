package com.shkgroups.provisioning.dto;

public record ProvisionPairingResponse(
        String orderId,
        String remoteJid,
        String pairingUrl,
        String messageText
) {}