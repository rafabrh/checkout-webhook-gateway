package com.shkgroups.pairing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PairingRepository extends JpaRepository<PairingSessionEntity, Long> {

    Optional<PairingSessionEntity> findByTokenHash(String tokenHash);

    Optional<PairingSessionEntity> findTopByOrderIdOrderByCreatedAtDesc(String orderId);

    Optional<PairingSessionEntity> findByOrderId(String orderId);

    Optional<PairingSessionEntity> findTopByInstanceOrderByCreatedAtDesc(String instance);
}
