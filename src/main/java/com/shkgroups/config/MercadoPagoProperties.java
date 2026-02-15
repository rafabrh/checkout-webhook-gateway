package com.shkgroups.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mercadopago")
public class MercadoPagoProperties {

    private String accessToken;
    private String baseUrl = "https://api.mercadopago.com";
    private String n8nWebhookUrl;
    private String webhookToken;
    private String notificationUrl;
    private String staticCheckoutUrl;

    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }

    public String resolveNotificationUrl() {
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            return notificationUrl.trim();
        }

        if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
            return null;
        }

        var base = n8nWebhookUrl.trim();

        if (webhookToken == null || webhookToken.isBlank()) {
            return base;
        }

        String sep = base.contains("?") ? "&" : "?";
        return base + sep + "token=" + URLEncoder.encode(webhookToken.trim(), StandardCharsets.UTF_8);
    }
}
