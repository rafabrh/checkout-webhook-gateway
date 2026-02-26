package com.shkgroups.ports.payments;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    MpPaymentData getPayment(String paymentId);

    String createCheckoutPreference(CreatePreferenceCommand command);

    record MpPaymentData(
            String id,
            String status,
            BigDecimal transactionAmount,
            String externalReference
    ) {}

    record CreatePreferenceCommand(
            String title,
            BigDecimal unitPrice,
            String externalReference,
            String notificationUrl
    ) {}
}
