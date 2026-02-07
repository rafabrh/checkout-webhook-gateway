package com.shkgroups.provisioning;

import com.shkgroups.pairing.PairingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PairingPublicController {

    private final PairingService pairingService;
    private final EvolutionClient evolutionClient;

    @GetMapping("/pair/{token}")
    public String page(@PathVariable String token, Model model) {

        var session = pairingService.validateForView(token);

        String base64 = session.getQrBase64();
        String dataUrl = session.getQrUrl();

        if (base64 == null || base64.isBlank()) {
            base64 = evolutionClient.getQrBase64(session.getInstance());
            dataUrl = "data:image/png;base64," + base64;
            pairingService.markReady(token, base64, dataUrl);
        }

        model.addAttribute("orderId", session.getOrderId());
        model.addAttribute("qrBase64", base64);
        model.addAttribute("qrUrl", dataUrl);
        model.addAttribute("expiresAt", session.getExpiresAt().toString());
        return "pairing";
    }
}