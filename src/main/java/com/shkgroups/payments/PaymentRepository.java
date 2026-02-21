package com.shkgroups.payments;

import com.shkgroups.payments.PaymentRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentsEntity, Long> {

    Optional<PaymentsEntity> findByPaymentId(String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentsEntity p where p.paymentId = :paymentId")
    Optional<PaymentsEntity> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);
}