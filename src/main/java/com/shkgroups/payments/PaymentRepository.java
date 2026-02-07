package com.shkgroups.payments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentsEntity, Long> {
    Optional<PaymentsEntity> findByPaymentId(String paymentId);
    Optional<PaymentsEntity> findByOrderId(String orderId);
}