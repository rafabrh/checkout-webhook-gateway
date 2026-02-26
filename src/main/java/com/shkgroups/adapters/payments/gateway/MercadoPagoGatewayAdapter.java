package com.shkgroups.adapters.payments.gateway;

import com.shkgroups.payments.mp.MercadoPagoClient;
import com.shkgroups.ports.payments.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MercadoPagoGatewayAdapter implements PaymentGatewayPort {

    private final MercadoPagoClient mercadoPagoClient;

    @Override
    public MpPaymentData getPayment(String paymentId) {
        var payment = mercadoPagoClient.getPayment(paymentId);
        if (payment == null) {
            return null;
        }
        return new MpPaymentData(
                payment.id(),
                payment.status(),
                payment.transactionAmount(),
                payment.externalReference()
        );
    }

    @Override
    public String createCheckoutPreference(CreatePreferenceCommand command) {
        var create = new MercadoPagoClient.MpPreferenceCreate(
                List.of(new MercadoPagoClient.MpPreferenceItem(
                        command.title(),
                        1,
                        command.unitPrice()
                )),
                command.externalReference(),
                command.notificationUrl()
        );

        var created = mercadoPagoClient.createPreference(create);
        if (created == null || created.initPoint() == null || created.initPoint().isBlank()) {
            throw new IllegalStateException("mp_preference_init_point_missing");
        }
        return created.initPoint();
    }
}
