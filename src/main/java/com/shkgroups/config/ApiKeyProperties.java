package com.shkgroups.config;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.api-key")
public record ApiKeyProperties(
        boolean enabled,
        String header,
        String key
) {
    @AssertTrue(message = "app.api-key.key não deve estar em branco quando app.api-key.enabled=true")
    public boolean isValid() {
        return !enabled || (key != null && !key.isBlank());
    }
}