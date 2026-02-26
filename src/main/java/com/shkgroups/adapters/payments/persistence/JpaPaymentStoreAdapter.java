package com.shkgroups.adapters.payments.persistence;

import com.shkgroups.payments.PaymentRepository;
import com.shkgroups.payments.PaymentsEntity;
import com.shkgroups.ports.payments.PaymentStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class JpaPaymentStoreAdapter implements PaymentStorePort {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void upsert(String paymentId, String orderId, String status, BigDecimal amount) {
        try {
            var existing = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);
            if (existing == null) {
                paymentRepository.save(PaymentsEntity.builder()
                        .paymentId(paymentId)
                        .orderId(orderId)
                        .status(status)
                        .amount(amount)
                        .build());
            } else {
                existing.setOrderId(orderId);
                existing.setStatus(status);
                existing.setAmount(amount);
                paymentRepository.save(existing);
            }
        } catch (DataIntegrityViolationException ex) {
            var existing = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);
            if (existing != null) {
                existing.setOrderId(orderId);
                existing.setStatus(status);
                existing.setAmount(amount);
                paymentRepository.save(existing);
            } else {
                log.info("payment_upsert_race paymentId={}", paymentId);
            }
        }
    }
}
