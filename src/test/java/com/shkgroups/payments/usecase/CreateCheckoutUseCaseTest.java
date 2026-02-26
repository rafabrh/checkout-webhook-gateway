package com.shkgroups.payments.usecase;

import com.shkgroups.application.payments.usecase.CreateCheckoutUseCase;
import com.shkgroups.config.properties.MercadoPagoProperties;
import com.shkgroups.payments.dto.CheckoutCreateRequest;
import com.shkgroups.ports.payments.OrderStorePort;
import com.shkgroups.ports.payments.PaymentGatewayPort;
import com.shkgroups.shared.domain.PlanId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCheckoutUseCaseTest {

    @Mock
    private OrderStorePort orderStorePort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    private MercadoPagoProperties mpProperties;
    private CreateCheckoutUseCase useCase;

    @BeforeEach
    void setUp() {
        mpProperties = new MercadoPagoProperties();
        useCase = new CreateCheckoutUseCase(orderStorePort, paymentGatewayPort, mpProperties);

        when(orderStorePort.createCreatedOrder(any(), anyString()))
                .thenReturn(new OrderStorePort.OrderData("order-1", "5511999999999@c.us", PlanId.AGENTE_IA_START, "whatsapp", "CREATED"));
    }

    @Test
    void shouldUseStaticCheckoutUrlWhenAccessTokenMissing() {
        mpProperties.setStaticCheckoutUrl("https://pay.example/checkout");

        var response = useCase.execute(request());

        assertThat(response.checkoutUrl()).isEqualTo("https://pay.example/checkout");
        assertThat(response.orderId()).isNotBlank();
        assertThat(response.messageText()).contains("https://pay.example/checkout");
        verify(paymentGatewayPort, never()).createCheckoutPreference(any());
    }

    @Test
    void shouldCreatePreferenceWhenAccessTokenPresent() {
        mpProperties.setAccessToken("token-123");
        mpProperties.setNotificationUrl("https://agent.example/v1/payments/mercadopago/notification?token=safe");
        when(paymentGatewayPort.createCheckoutPreference(any())).thenReturn("https://mp.example/init");

        var response = useCase.execute(request());

        assertThat(response.checkoutUrl()).isEqualTo("https://mp.example/init");
        assertThat(response.orderId()).isNotBlank();

        ArgumentCaptor<PaymentGatewayPort.CreatePreferenceCommand> commandCaptor =
                ArgumentCaptor.forClass(PaymentGatewayPort.CreatePreferenceCommand.class);
        verify(paymentGatewayPort).createCheckoutPreference(commandCaptor.capture());
        assertThat(commandCaptor.getValue().externalReference()).isEqualTo(response.orderId());
        assertThat(commandCaptor.getValue().title()).isEqualTo(PlanId.AGENTE_IA_START.getTitle());
    }

    @Test
    void shouldMarkOrderCanceledWhenPreferenceCreationFails() {
        mpProperties.setAccessToken("token-123");
        mpProperties.setNotificationUrl("https://agent.example/v1/payments/mercadopago/notification?token=safe");
        when(paymentGatewayPort.createCheckoutPreference(any())).thenThrow(new IllegalStateException("mp_down"));

        assertThatThrownBy(() -> useCase.execute(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mp_down");

        verify(orderStorePort).markCanceled(anyString());
    }

    private CheckoutCreateRequest request() {
        return new CheckoutCreateRequest(
                "instance-a",
                "5511999999999@c.us",
                PlanId.AGENTE_IA_START,
                "whatsapp",
                new CheckoutCreateRequest.Customer("Cliente")
        );
    }
}
