package com.shkgroups.pairing;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PairingService {

    private static final int RAW_TOKEN_BYTES = 32;
    private static final int MAX_TOKEN_LENGTH = 256;
    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final PairingRepository pairingRepository;

    public PairingLink create(String orderId, String instance, String remoteJid, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("invalid_ttl");
        }

        var now = OffsetDateTime.now();

        for (int attempt = 1; attempt <= 3; attempt++) {
            String rawToken = generateToken();
            String tokenHash = sha256Hex(rawToken);

            try {
                pairingRepository.save(PairingSessionEntity.builder()
                        .orderId(orderId)
                        .instance(instance)
                        .remoteJid(remoteJid)
                        .rawToken(rawToken)
                        .tokenHash(tokenHash)
                        .status(PairingStatus.NEW)
                        .createdAt(now)
                        .updatedAt(now)
                        .expiresAt(now.plus(ttl))
                        .build());

                return new PairingLink(rawToken, "/pair/" + rawToken);
            } catch (DataIntegrityViolationException e) {
                if (attempt == 3) throw e;
            }
        }

        throw new IllegalStateException("could_not_create_pairing_session");
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public PairingSessionEntity validateForView(String rawToken) {
        var session = getSessionOrThrow(rawToken);

        expireIfNeeded(session);

        return switch (session.getStatus()) {
            case NEW, READY -> session;
            case PAIRED -> throw new IllegalArgumentException("already_paired");
            case EXPIRED -> throw new IllegalArgumentException("expired_token");
            case FAILED -> throw new IllegalArgumentException("pairing_failed");
        };
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public PairingSessionEntity markReady(String rawToken, String qrBase64, String qrUrl) {
        var session = getSessionOrThrow(rawToken);

        expireIfNeeded(session);

        if (session.getStatus() == PairingStatus.PAIRED) {
            return session;
        }

        session.setQrBase64(qrBase64);
        session.setQrUrl(qrUrl);

        if (session.getStatus() == PairingStatus.NEW || session.getStatus() == PairingStatus.FAILED) {
            session.setStatus(PairingStatus.READY);
        }

        session.touchUpdate();
        return session;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public PairingSessionEntity markPaired(String rawToken) {
        var session = getSessionOrThrow(rawToken);

        expireIfNeeded(session);

        session.setStatus(PairingStatus.PAIRED);
        session.touchUpdate();
        return session;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public PairingSessionEntity markFailed(String rawToken) {
        var session = getSessionOrThrow(rawToken);

        expireIfNeeded(session);

        if (session.getStatus() == PairingStatus.PAIRED) {
            return session;
        }

        session.setStatus(PairingStatus.FAILED);
        session.touchUpdate();
        return session;
    }

    private PairingSessionEntity getSessionOrThrow(String rawToken) {
        validateRawToken(rawToken);
        var hash = sha256Hex(rawToken);
        return pairingRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("invalid_token"));
    }

    private static void expireIfNeeded(PairingSessionEntity session) {
        if (session.getStatus() == PairingStatus.PAIRED) return;

        if (OffsetDateTime.now().isAfter(session.getExpiresAt())) {
            if (session.getStatus() != PairingStatus.EXPIRED) {
                session.setStatus(PairingStatus.EXPIRED);
                session.touchUpdate();
            }
            throw new IllegalArgumentException("expired_token");
        }
    }

    private static void validateRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalArgumentException("invalid_token");
        }

    }
    @Transactional
    public PairingLink getOrCreateActive(String orderId, String instance, String remoteJid, Duration ttl) {
        var now = OffsetDateTime.now();

        var existingOpt = pairingRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId);
        if (existingOpt.isPresent()) {
            var s = existingOpt.get();

            if (s.getStatus() == PairingStatus.PAIRED) {
                throw new IllegalArgumentException("already_paired");
            }

            boolean notExpired = now.isBefore(s.getExpiresAt());
            boolean reusableStatus = (s.getStatus() == PairingStatus.NEW || s.getStatus() == PairingStatus.READY || s.getStatus() == PairingStatus.FAILED);

            if (notExpired && reusableStatus && s.getRawToken() != null && !s.getRawToken().isBlank()) {
                return new PairingLink(s.getRawToken(), "/pair/" + s.getRawToken());
            }
        }

        return create(orderId, instance, remoteJid, ttl);
    }


    private static String generateToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    private static String sha256Hex(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HEX.formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record PairingLink(String token, String urlPath) {}


}