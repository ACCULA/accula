package org.accula.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("accula.webhook")
@Data
public class WebhookProperties {
    private String url;
    private String secret;
}
