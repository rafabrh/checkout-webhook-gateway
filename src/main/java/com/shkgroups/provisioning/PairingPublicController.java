package com.shkgroups.provisioning;

import com.shkgroups.pairing.PairingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PairingPublicController {

    private final PairingService pairingService;
    private final EvolutionClient evolutionClient;

    @GetMapping("/pair/{token}")
    public String page(@PathVariable String token, Model model) {

        var session = pairingService.validateForView(token);

        String base64 = session.getQrBase64();
        String dataUrl = session.getQrUrl();

        boolean pending = false;
        String pendingMessage = null;

        if (base64 == null || base64.isBlank()) {
            try {
                base64 = evolutionClient.getQrBase64(session.getInstance());
                dataUrl = "data:image/png;base64," + base64;
                pairingService.markReady(token, base64, dataUrl);

            } catch (Exception ex) {
                log.warn("QR not ready yet. token={}, instance={}, orderId={}",
                        token, session.getInstance(), session.getOrderId(), ex);

                pending = true;
                pendingMessage = "Estamos preparando seu QR Code. Aguarde alguns segundos…";
            }
        }

        model.addAttribute("orderId", session.getOrderId());
        model.addAttribute("qrBase64", base64);
        model.addAttribute("qrUrl", dataUrl);
        model.addAttribute("expiresAt", session.getExpiresAt().toString());
        model.addAttribute("pending", pending);
        model.addAttribute("pendingMessage", pendingMessage);
        model.addAttribute("instance", session.getInstance());

        return "pairing";
    }
}