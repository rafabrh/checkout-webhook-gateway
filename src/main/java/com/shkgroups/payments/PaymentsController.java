package com.shkgroups.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.payments.dto.CheckoutCreateResponse;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/payments")
public class PaymentsController {

    private final CheckoutService checkoutService;
    private final MercadoPagoWebhookService webhookService;

    @PostMapping("/checkout")
    public CheckoutCreateResponse checkout(@RequestBody @Valid CheckoutCreateRequest req) {
        return checkoutService.createCheckout(req);
    }

    @RequestMapping(
            value = "/mercadopago/notification",
            method = {RequestMethod.POST, RequestMethod.GET}
    )
    public MercadoPagoWebhookResponse mpNotification(
            @RequestParam(name = "token", required = false) String token,
            @RequestParam MultiValueMap<String, String> query,
            @RequestBody(required = false) JsonNode body
    ) {
        var paymentId = extractPaymentId(body, query);
        return webhookService.process(token, paymentId);
    }

    private String extractPaymentId(JsonNode body, MultiValueMap<String, String> query) {
        String fromQuery = firstNonBlank(query.getFirst("data.id"), query.getFirst("id"));
        if (fromQuery != null) return fromQuery;

        if (body == null) return null;

        JsonNode dataId = body.path("data").path("id");
        if (!dataId.isMissingNode() && !dataId.isNull() && !dataId.asText().isBlank()) {
            return dataId.asText();
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
