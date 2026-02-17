package com.shkgroups.provisioning;

import com.shkgroups.config.properties.AgentProperties;
import com.shkgroups.orders.OrderRepository;
import com.shkgroups.pairing.PairingRepository;
import com.shkgroups.pairing.PairingService;
import com.shkgroups.pairing.PairingStatus;
import com.shkgroups.orders.OrderStatus;
import com.shkgroups.payments.PaymentRepository;
import com.shkgroups.provisioning.dto.ProvisionPairingRequest;
import com.shkgroups.provisioning.dto.ProvisionPairingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROVISIONED) {
            throw new IllegalArgumentException("order_not_paid");
        }

        paymentRepository.findByPaymentId(req.paymentId())
                .orElseThrow(() -> new IllegalArgumentException("payment_not_found"));

        if (req.plan() != null && !req.plan().isBlank() && !req.plan().equalsIgnoreCase(order.getPlan())) {
            log.warn("Plan mismatch. req.plan={} order.plan={}", req.plan(), order.getPlan());
        }

        pairingRepository.findByOrderId(order.getOrderId()).ifPresent(existing -> {
            if (existing.getStatus() != PairingStatus.PAIRED) {
                existing.setStatus(PairingStatus.EXPIRED);
                existing.touchUpdate();
                pairingRepository.save(existing);
            }
        });

        var link = pairingService.create(
                order.getOrderId(),
                order.getInstance(),
                order.getRemoteJid(),
                DEFAULT_TTL
        );

        var session = pairingRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("pairing_session_not_found_after_create"));

        var pairingUrl = agentProps.getBaseUrl() + link.urlPath();
        var messageText = "Pagamento confirmado ✅ Pareie aqui: " + pairingUrl;

        if (order.getStatus() == OrderStatus.PAID) {
            order.setStatus(OrderStatus.PROVISIONED);
            order.touchUpdate();
            orderRepository.save(order);
        }

        return new ProvisionPairingResponse(
                order.getOrderId(),
                order.getRemoteJid(),
                pairingUrl,
                messageText,
                session.getExpiresAt()
        );
    }
}
