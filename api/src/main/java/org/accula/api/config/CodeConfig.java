package org.accula.api.config;

import org.accula.api.code.RepositoryProvider;
import org.accula.api.code.RepositoryManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * @author Vadim Dyachkov
 */
@Configuration
public class CodeConfig {
    @Bean
    public RepositoryProvider repositoryManager(@Value("${accula.reposPath}") final String reposPath) {
        final File root = new File(reposPath);
        return new RepositoryManager(root);
    }
}
