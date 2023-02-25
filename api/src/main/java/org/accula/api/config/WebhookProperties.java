package org.accula.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vadim Dyachkov
 */
@ConfigurationProperties("accula.webhook")
public record WebhookProperties(String url, String secret, Boolean sslEnabled) {
}
