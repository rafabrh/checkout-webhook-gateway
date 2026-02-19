package com.shkgroups.pairing;

import com.shkgroups.pairing.PairingService;
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
    private String publicBaseUrl; // https://agent.shkgroups.com

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

    public record PairingLinkResponse(String token, String url) {}
}
