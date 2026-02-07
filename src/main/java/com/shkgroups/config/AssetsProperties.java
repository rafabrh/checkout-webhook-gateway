package com.shkgroups.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.assets")
public class AssetsProperties {

    private String catalogImageUrl;
    private String catalogCaption;
}