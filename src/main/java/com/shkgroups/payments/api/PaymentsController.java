package com.shkgroups.payments.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.shkgroups.application.payments.usecase.CreateCheckoutUseCase;
import com.shkgroups.application.payments.usecase.ProcessMpWebhookUseCase;
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

    private final CreateCheckoutUseCase createCheckoutUseCase;
    private final ProcessMpWebhookUseCase processMpWebhookUseCase;

    @PostMapping("/checkout")
    public CheckoutCreateResponse checkout(@RequestBody @Valid CheckoutCreateRequest req) {
        return createCheckoutUseCase.execute(req);
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
        return processMpWebhookUseCase.execute(token, paymentId);
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
