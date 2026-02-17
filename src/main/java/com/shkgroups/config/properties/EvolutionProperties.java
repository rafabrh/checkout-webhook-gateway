package com.shkgroups.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.evolution")
public class EvolutionProperties {
    private String baseUrl;   // https://evo.dominio.com
    private String apiKey;
    private String instancePathPrefix = "/instance";
}