package com.shkgroups.payments.usecase;

import com.shkgroups.application.payments.usecase.ProcessMpWebhookUseCase;
import com.shkgroups.config.properties.MercadoPagoProperties;
import com.shkgroups.payments.domain.MpDecision;
import com.shkgroups.ports.payments.OrderStorePort;
import com.shkgroups.ports.payments.PaymentGatewayPort;
import com.shkgroups.ports.payments.PaymentStorePort;
import com.shkgroups.shared.domain.PlanId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessMpWebhookUseCaseTest {

    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private PaymentStorePort paymentStorePort;
    @Mock
    private OrderStorePort orderStorePort;

    private ProcessMpWebhookUseCase useCase;

    @BeforeEach
    void setUp() {
        var mpProperties = new MercadoPagoProperties();
        mpProperties.setWebhookToken("token-ok");
        useCase = new ProcessMpWebhookUseCase(mpProperties, paymentGatewayPort, paymentStorePort, orderStorePort);
    }

    @Test
    void shouldIgnoreWhenTokenInvalid() {
        var response = useCase.execute("wrong", "123");

        assertThat(response.decision()).isEqualTo(MpDecision.IGNORE);
        assertThat(response.reason()).isEqualTo("invalid_webhook_token");
        verifyNoInteractions(paymentGatewayPort, paymentStorePort, orderStorePort);
    }

    @Test
    void shouldIgnoreWhenPaymentIdMissing() {
        var response = useCase.execute("token-ok", " ");

        assertThat(response.decision()).isEqualTo(MpDecision.IGNORE);
        assertThat(response.reason()).isEqualTo("payment_id_missing");
        verifyNoInteractions(paymentGatewayPort, paymentStorePort, orderStorePort);
    }

    @Test
    void shouldIgnoreWhenPaymentNull() {
        when(paymentGatewayPort.getPayment("p-1")).thenReturn(null);

        var response = useCase.execute("token-ok", "p-1");

        assertThat(response.reason()).isEqualTo("mp_payment_null");
        assertThat(response.payment().id()).isEqualTo("p-1");
        verifyNoInteractions(paymentStorePort, orderStorePort);
    }

    @Test
    void shouldIgnoreNoExternalReferenceWithoutPersistingPayment() {
        when(paymentGatewayPort.getPayment("p-1"))
                .thenReturn(new PaymentGatewayPort.MpPaymentData("p-1", "approved", new BigDecimal("99.90"), null));

        var response = useCase.execute("token-ok", "p-1");

        assertThat(response.reason()).isEqualTo("no_external_reference");
        verifyNoInteractions(paymentStorePort, orderStorePort);
    }

    @Test
    void shouldIgnoreWhenOrderNotFoundAfterUpsert() {
        when(paymentGatewayPort.getPayment("p-1"))
                .thenReturn(new PaymentGatewayPort.MpPaymentData("p-1", "approved", new BigDecimal("99.90"), "o-1"));
        when(orderStorePort.findByOrderId("o-1")).thenReturn(Optional.empty());

        var response = useCase.execute("token-ok", "p-1");

        assertThat(response.reason()).isEqualTo("order_not_found");
        verify(paymentStorePort).upsert("p-1", "o-1", "approved", new BigDecimal("99.90"));
    }

    @Test
    void shouldReturnProvisionAndMarkPaidWhenApprovedAndAmountMatches() {
        when(paymentGatewayPort.getPayment("p-1"))
                .thenReturn(new PaymentGatewayPort.MpPaymentData("p-1", "approved", new BigDecimal("99.90"), "o-1"));
        when(orderStorePort.findByOrderId("o-1"))
                .thenReturn(Optional.of(new OrderStorePort.OrderData("o-1", "5511@c.us", PlanId.AGENTE_IA_START, "whatsapp", "CREATED")));

        var response = useCase.execute("token-ok", "p-1");

        assertThat(response.decision()).isEqualTo(MpDecision.PROVISION);
        assertThat(response.reason()).isNull();
        verify(orderStorePort).markPaidIfCreated("o-1");
    }

    @Test
    void shouldReturnWaitPaymentForPendingStatus() {
        when(paymentGatewayPort.getPayment("p-1"))
                .thenReturn(new PaymentGatewayPort.MpPaymentData("p-1", "pending", new BigDecimal("99.90"), "o-1"));
        when(orderStorePort.findByOrderId("o-1"))
                .thenReturn(Optional.of(new OrderStorePort.OrderData("o-1", "5511@c.us", PlanId.AGENTE_IA_START, "whatsapp", "CREATED")));

        var response = useCase.execute("token-ok", "p-1");

        assertThat(response.decision()).isEqualTo(MpDecision.WAIT_PAYMENT);
        assertThat(response.reason()).isEqualTo("mp_status:pending");
        verify(orderStorePort, never()).markPaidIfCreated(anyString());
    }

    @Test
    void shouldIgnoreOnAmountMismatch() {
        when(paymentGatewayPort.getPayment("p-1"))
                .thenReturn(new PaymentGatewayPort.MpPaymentData("p-1", "approved", new BigDecimal("10.00"), "o-1"));
        when(orderStorePort.findByOrderId("o-1"))
                .thenReturn(Optional.of(new OrderStorePort.OrderData("o-1", "5511@c.us", PlanId.AGENTE_IA_START, "whatsapp", "CREATED")));

        var response = useCase.execute("token-ok", "p-1");

        assertThat(response.reason()).isEqualTo("amount_mismatch");
        assertThat(response.decision()).isEqualTo(MpDecision.IGNORE);
        verify(orderStorePort, never()).markPaidIfCreated(anyString());
    }
}
