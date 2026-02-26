package com.shkgroups.payments;

import com.shkgroups.application.payments.usecase.ProcessMpWebhookUseCase;
import com.shkgroups.payments.dto.MercadoPagoWebhookRequest;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MercadoPagoWebhookService {

    private final ProcessMpWebhookUseCase processMpWebhookUseCase;

    public MercadoPagoWebhookResponse process(MercadoPagoWebhookRequest req) {
        var token = (req != null && req.mp() != null) ? req.mp().token() : null;
        var paymentId = (req != null && req.mp() != null) ? req.mp().paymentId() : null;
        return process(token, paymentId);
    }

    public MercadoPagoWebhookResponse process(String token, String paymentId) {
        return processMpWebhookUseCase.execute(token, paymentId);
    }
}
