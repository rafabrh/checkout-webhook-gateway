package com.shkgroups.adapters.payments.persistence;

import com.shkgroups.orders.OrderEntity;
import com.shkgroups.orders.OrderRepository;
import com.shkgroups.orders.OrderStatus;
import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.ports.payments.OrderStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaOrderStoreAdapter implements OrderStorePort {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderData createCreatedOrder(CheckoutCreateRequest request, String orderId) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        OrderEntity saved = orderRepository.save(OrderEntity.builder()
                .orderId(orderId)
                .instance(request.instance())
                .remoteJid(request.remoteJid())
                .plan(request.plan())
                .channel(request.channel())
                .status(OrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return toData(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderData> findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId).map(this::toData);
    }

    @Override
    @Transactional
    public void markPaidIfCreated(String orderId) {
        orderRepository.findByOrderId(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CREATED) {
                order.setStatus(OrderStatus.PAID);
                order.touchUpdate();
                orderRepository.save(order);
            }
        });
    }

    @Override
    @Transactional
    public void markCanceled(String orderId) {
        orderRepository.findByOrderId(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CREATED) {
                order.setStatus(OrderStatus.CANCELED);
                order.touchUpdate();
                orderRepository.save(order);
            }
        });
    }

    private OrderData toData(OrderEntity entity) {
        return new OrderData(
                entity.getOrderId(),
                entity.getRemoteJid(),
                entity.getPlan(),
                entity.getChannel(),
                entity.getStatus().name()
        );
    }
}
