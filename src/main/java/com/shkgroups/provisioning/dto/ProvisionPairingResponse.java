package com.shkgroups.provisioning.dto;

import java.time.OffsetDateTime;

public record ProvisionPairingResponse(
        String orderId,
        String remoteJid,
        String pairingUrl,
        String messageText,
        OffsetDateTime expiresAt
) {}