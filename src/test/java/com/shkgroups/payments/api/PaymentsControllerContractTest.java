package com.shkgroups.payments.api;

import com.shkgroups.application.payments.usecase.CreateCheckoutUseCase;
import com.shkgroups.application.payments.usecase.ProcessMpWebhookUseCase;
import com.shkgroups.config.properties.ApiKeyProperties;
import com.shkgroups.payments.domain.MpDecision;
import com.shkgroups.payments.dto.CheckoutCreateResponse;
import com.shkgroups.payments.dto.MercadoPagoWebhookResponse;
import com.shkgroups.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentsController.class)
@Import({SecurityConfig.class, PaymentsControllerContractTest.TestConfig.class})
class PaymentsControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCheckoutUseCase createCheckoutUseCase;

    @MockitoBean
    private ProcessMpWebhookUseCase processMpWebhookUseCase;

    @Test
    void checkoutShouldKeepResponseContract() throws Exception {
        when(createCheckoutUseCase.execute(any())).thenReturn(
                new CheckoutCreateResponse("o-1", "https://pay.example/o-1", "Segue seu link de pagamento (Pix/Cartão): https://pay.example/o-1")
        );

        mockMvc.perform(post("/v1/payments/checkout")
                        .contentType(APPLICATION_JSON)
                        .header("X-Api-Key", "test-key")
                        .content("""
                                {
                                  "instance": "inst-1",
                                  "remoteJid": "5511999999999@c.us",
                                  "plan": "agente_ia_start",
                                  "channel": "whatsapp",
                                  "customer": {"name": "Cliente"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("o-1"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://pay.example/o-1"))
                .andExpect(jsonPath("$.messageText").exists());
    }

    @Test
    void webhookGetShouldBePublicAndReturnUseCaseResponse() throws Exception {
        when(processMpWebhookUseCase.execute(eq("bad"), eq("123"))).thenReturn(
                new MercadoPagoWebhookResponse(MpDecision.IGNORE, null, null, "invalid_webhook_token")
        );

        mockMvc.perform(get("/v1/payments/mercadopago/notification")
                        .queryParam("token", "bad")
                        .queryParam("data.id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("IGNORE"))
                .andExpect(jsonPath("$.reason").value("invalid_webhook_token"));
    }

    @Test
    void webhookPostShouldExtractPaymentIdFromBody() throws Exception {
        when(processMpWebhookUseCase.execute(eq("token-ok"), eq("p-1"))).thenReturn(
                new MercadoPagoWebhookResponse(
                        MpDecision.WAIT_PAYMENT,
                        null,
                        new MercadoPagoWebhookResponse.PaymentRef("p-1"),
                        "mp_status:pending"
                )
        );

        mockMvc.perform(post("/v1/payments/mercadopago/notification")
                        .queryParam("token", "token-ok")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {"id": "p-1"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("WAIT_PAYMENT"))
                .andExpect(jsonPath("$.payment.id").value("p-1"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        ApiKeyProperties apiKeyProperties() {
            return new ApiKeyProperties(true, "X-Api-Key", "test-key");
        }
    }
}
