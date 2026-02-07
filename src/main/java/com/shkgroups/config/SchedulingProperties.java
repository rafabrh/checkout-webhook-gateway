package com.shkgroups.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.scheduling")
public class SchedulingProperties {
    private String staticCallUrl;
}