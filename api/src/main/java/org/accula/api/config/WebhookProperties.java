package org.accula.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vadim Dyachkov
 */
@ConfigurationProperties("accula.webhook")
@Data
public class WebhookProperties {
    private String url;
    private String secret;
}
