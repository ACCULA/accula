package org.accula.api.config;

import org.accula.api.code.RepositoryManager;
import org.accula.api.code.RepositoryManagerImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class CodeConfiguration {

    @Bean
    public RepositoryManager repositoryManager(@Value("${accula.reposPath}") final String reposPath) {
        final File root = new File(reposPath);
        return new RepositoryManagerImpl(root);
    }
}
