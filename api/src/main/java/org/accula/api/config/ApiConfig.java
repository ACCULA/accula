package org.accula.api.config;

import org.accula.api.code.RepositoryProvider;
import org.accula.api.code.RepositoryManager;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.PrimitiveCloneDetector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * @author Vadim Dyachkov
 */
@Configuration
@EnableConfigurationProperties(WebhookProperties.class)
public class ApiConfig {
    @Bean
    public RepositoryProvider repositoryManager(@Value("${accula.reposPath}") final String reposPath) {
        final File root = new File(reposPath);
        return new RepositoryManager(root);
    }

    @Bean
    public CloneDetector cloneDetector() {
        return new PrimitiveCloneDetector(1, 1);
    }
}
