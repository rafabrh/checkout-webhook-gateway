package com.shkgroups.pairing;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "pairing_sessions",
        indexes = {
                @Index(name = "idx_pairing_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_pairing_expires_at", columnList = "expires_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PairingSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "instance", nullable = false, length = 60)
    private String instance;

    @Column(name = "remote_jid", length = 80)
    private String remoteJid;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PairingStatus status;

    @Lob
    @Column(name = "qr_base64", columnDefinition = "TEXT")
    private String qrBase64;

    @Column(name = "qr_url", length = 1024)
    private String qrUrl;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "raw_token", length = 256)
    private String rawToken;


    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = PairingStatus.NEW;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void touchUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}