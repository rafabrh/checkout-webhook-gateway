package com.shkgroups.ports.payments;

import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.shared.domain.PlanId;

import java.util.Optional;

public interface OrderStorePort {

    OrderData createCreatedOrder(CheckoutCreateRequest request, String orderId);

    Optional<OrderData> findByOrderId(String orderId);

    void markPaidIfCreated(String orderId);

    void markCanceled(String orderId);

    record OrderData(
            String orderId,
            String remoteJid,
            PlanId plan,
            String channel,
            String status
    ) {}
}
