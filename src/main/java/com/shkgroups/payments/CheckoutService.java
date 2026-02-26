package com.shkgroups.payments;

import com.shkgroups.application.payments.usecase.CreateCheckoutUseCase;
import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.payments.dto.CheckoutCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CreateCheckoutUseCase createCheckoutUseCase;

    public CheckoutCreateResponse createCheckout(CheckoutCreateRequest req) {
        return createCheckoutUseCase.execute(req);
    }
}
