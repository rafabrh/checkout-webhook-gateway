package com.shkgroups.pairing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/pairing")
public class PairingInternalController {

    private final PairingService pairingService;

    @Value("${app.public-base-url}")
    private String publicBaseUrl; // ex: https://agent.shkgroups.com

    @PostMapping("/orders/{orderId}/create")
    public ResponseEntity<PairingLinkResponse> create(
            @PathVariable String orderId,
            @RequestBody PairingCreateRequest body,
            @RequestParam(defaultValue = "20") long ttlMinutes
    ) {
        var link = pairingService.create(orderId, body.instance(), body.remoteJid(), Duration.ofMinutes(ttlMinutes));
        return ResponseEntity.ok(new PairingLinkResponse(
                link.token(),
                publicBaseUrl + link.urlPath()
        ));
    }

    @PostMapping("/orders/{orderId}/resend")
    public ResponseEntity<PairingLinkResponse> resend(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "20") long ttlMinutes
    ) {
        var link = pairingService.resendLink(orderId, Duration.ofMinutes(ttlMinutes));
        return ResponseEntity.ok(new PairingLinkResponse(
                link.token(),
                publicBaseUrl + link.urlPath()
        ));
    }

    @PostMapping("/tokens/{token}/ready")
    public ResponseEntity<PairingStatusResponse> ready(
            @PathVariable String token,
            @RequestBody PairingReadyRequest body
    ) {
        var session = pairingService.markReady(
                token,
                body.qrPayload(),
                body.pairingCode(),
                body.qrBase64(),
                body.qrUrl()
        );
        return ResponseEntity.ok(PairingStatusResponse.from(session));
    }

    @PostMapping("/instances/{instance}/paired")
    public ResponseEntity<PairingStatusResponse> pairedByInstance(@PathVariable String instance) {
        var session = pairingService.markPairedByInstance(instance);
        return ResponseEntity.ok(PairingStatusResponse.from(session));
    }

    @GetMapping("/tokens/{token}")
    public ResponseEntity<PairingStatusResponse> get(@PathVariable String token) {
        var session = pairingService.validateForView(token);
        return ResponseEntity.ok(PairingStatusResponse.from(session));
    }

    public record PairingLinkResponse(String token, String url) {}

    public record PairingCreateRequest(String instance, String remoteJid) {}

    public record PairingReadyRequest(
            String qrPayload,
            String pairingCode,
            String qrBase64,
            String qrUrl
    ) {}

    public record PairingStatusResponse(
            String orderId,
            String instance,
            String remoteJid,
            String status,
            String qrPayload,
            String pairingCode,
            String qrBase64,
            String qrUrl,
            String expiresAt
    ) {
        static PairingStatusResponse from(PairingSessionEntity s) {
            return new PairingStatusResponse(
                    s.getOrderId(),
                    s.getInstance(),
                    s.getRemoteJid(),
                    s.getStatus().name(),
                    s.getQrPayload(),
                    s.getPairingCode(),
                    s.getQrBase64(),
                    s.getQrUrl(),
                    s.getExpiresAt() != null ? s.getExpiresAt().toString() : null
            );
        }
    }
}