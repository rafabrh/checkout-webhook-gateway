package com.shkgroups.provisioning;

import com.shkgroups.config.properties.AgentProperties;
import com.shkgroups.orders.OrderRepository;
import com.shkgroups.orders.OrderStatus;
import com.shkgroups.pairing.PairingRepository;
import com.shkgroups.pairing.PairingService;
import com.shkgroups.pairing.PairingStatus;
import com.shkgroups.payments.PaymentRepository;
import com.shkgroups.provisioning.dto.ProvisionPairingRequest;
import com.shkgroups.provisioning.dto.ProvisionPairingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisioningService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PairingRepository pairingRepository;
    private final PairingService pairingService;
    private final AgentProperties agentProps;

    @Transactional
    public ProvisionPairingResponse provisionPairing(ProvisionPairingRequest req) {

        var order = orderRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> domainError(HttpStatus.NOT_FOUND, "order_not_found"));

        assertOrderProvisionable(order.getStatus());

        paymentRepository.findByPaymentId(req.paymentId())
                .orElseThrow(() -> domainError(HttpStatus.NOT_FOUND, "payment_not_found"));

        if (req.plan() != null && req.plan() != order.getPlan()) {
            log.warn("Plan mismatch. orderId={} req.plan={} order.plan={}",
                    order.getOrderId(), req.plan(), order.getPlan());
            throw domainError(HttpStatus.CONFLICT, "plan_mismatch");
        }

        expireNonPairedSessions(order.getOrderId());

        var orderId = order.getOrderId();
        var pairingInstance = computePairingInstance(orderId);

        var existing = pairingRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId).orElse(null);

        PairingService.PairingLink link;
        if (existing == null) {
            link = pairingService.create(orderId, pairingInstance, order.getRemoteJid(), DEFAULT_TTL);
        } else {
            link = pairingService.resendLink(orderId, DEFAULT_TTL);
        }

        var session = pairingRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> domainError(HttpStatus.INTERNAL_SERVER_ERROR,
                        "pairing_session_not_found_after_create"));

        session.setInstance(pairingInstance);
        session.setQrBase64(null);
        session.setQrUrl(null);
        session.setStatus(PairingStatus.NEW);
        session.touchUpdate();
        pairingRepository.save(session);

        var pairingUrl = agentProps.getBaseUrl() + link.urlPath();
        var messageText = "Pagamento confirmado ✅ Pareie aqui: " + pairingUrl;

        if (order.getStatus() == OrderStatus.PAID) {
            order.setStatus(OrderStatus.PROVISIONED);
            order.touchUpdate();
            orderRepository.save(order);
        }

        log.info("Provision pairing ready. orderId={} pairingInstance={} expiresAt={}",
                orderId, pairingInstance, session.getExpiresAt());

        return new ProvisionPairingResponse(
                orderId,
                order.getRemoteJid(),
                pairingUrl,
                messageText,
                session.getExpiresAt()
        );
    }

    private static String computePairingInstance(String orderId) {
        var safe = orderId == null ? "" : orderId.replace("-", "").toLowerCase();
        if (safe.length() > 24) safe = safe.substring(0, 24);
        return "shk_" + safe;
    }

    private void assertOrderProvisionable(OrderStatus status) {
        if (status != OrderStatus.PAID && status != OrderStatus.PROVISIONED) {
            throw domainError(HttpStatus.CONFLICT, "order_not_paid");
        }
    }

    private void expireNonPairedSessions(String orderId) {
        pairingRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getStatus() != PairingStatus.PAIRED) {
                existing.setStatus(PairingStatus.EXPIRED);
                existing.touchUpdate();
                pairingRepository.save(existing);
            }
        });
    }

    private RuntimeException domainError(HttpStatus status, String code) {
        return new ProvisioningException(status.value(), code);
    }

    public static class ProvisioningException extends RuntimeException {
        private final int httpStatus;
        private final String code;

        public ProvisioningException(int httpStatus, String code) {
            super(code);
            this.httpStatus = httpStatus;
            this.code = code;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public String getCode() {
            return code;
        }
    }
}