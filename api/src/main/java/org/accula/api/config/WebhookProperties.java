package org.accula.api.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * @author Vadim Dyachkov
 */
@ConstructorBinding
@ConfigurationProperties("accula.webhook")
@Value
public class WebhookProperties {
    String url;
    String secret;
}
