package com.shkgroups.ports.payments;

import java.math.BigDecimal;

public interface PaymentStorePort {

    void upsert(String paymentId, String orderId, String status, BigDecimal amount);
}
