package com.shkgroups.config;

import com.shkgroups.config.properties.EvolutionProperties;
import com.shkgroups.config.properties.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebClientConfig {

    @Bean(name = "mpWebClient")
    public WebClient mercadoPagoWebClient(MercadoPagoProperties mpProps) {

        String baseUrl = normalizeOrDefault(
                mpProps.getBaseUrl(),
                "https://api.mercadopago.com"
        );

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(defaultExchangeStrategies())
                .build();
    }

    @Bean(name = "evolutionWebClient")
    public WebClient evolutionApiWebClient(EvolutionProperties evoProps) {

        String baseUrl = normalizeOrDefault(
                evoProps.getBaseUrl(),
                ""
        );

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(defaultExchangeStrategies())
                .build();
    }

    private static String normalizeOrDefault(String value, String def) {
        if (value == null) return def;
        String v = value.trim();
        return v.isBlank() ? def : v;
    }

    private static ExchangeStrategies defaultExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }
}
