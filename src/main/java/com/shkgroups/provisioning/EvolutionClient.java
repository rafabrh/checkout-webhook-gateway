package com.shkgroups.provisioning;

import com.shkgroups.config.EvolutionProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class EvolutionClient {

    private final WebClient webClient;
    private final EvolutionProperties props;

    public EvolutionClient(
            @Qualifier("evolutionWebClient") WebClient webClient,
            EvolutionProperties props
    ) {
        this.webClient = webClient;
        this.props = props;
    }

    public String getQrBase64(String instance) {
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            throw new IllegalStateException("evolution_base_url_is_required");
        }
        if (instance == null || instance.isBlank()) {
            throw new IllegalArgumentException("instance_is_required");
        }

        String path = props.getInstancePathPrefix() + "/" + instance + "/qrcode";

        Map<?, ?> json = webClient.get()
                .uri(path)
                .headers(this::applyAuth)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (json == null) throw new IllegalStateException("evolution_qr_empty_response");

        String base64 = firstString(json, "base64", "qrcode", "qr", "qrCode", "qrCodeBase64");

        if (base64 == null) {
            Object qrcodeObj = json.get("qrcode");
            if (qrcodeObj instanceof Map<?, ?> nested) {
                base64 = firstString(nested, "base64", "qr", "qrcode");
            }
        }

        if (base64 == null || base64.isBlank()) throw new IllegalStateException("evolution_qr_field_not_found");

        String prefix = "data:image/png;base64,";
        if (base64.startsWith(prefix)) base64 = base64.substring(prefix.length());

        return base64;
    }

    private void applyAuth(HttpHeaders h) {
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            h.set("x-api-key", props.getApiKey().trim());
        }
    }

    private static String firstString(Map<?, ?> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }
}
