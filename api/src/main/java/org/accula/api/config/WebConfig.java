package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.RepositoryManager;
import org.accula.api.code.RepositoryProvider;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.PrimitiveCloneDetector;
import org.accula.api.github.api.GithubClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@SpringBootConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(WebhookProperties.class)
public class WebConfig implements WebFluxConfigurer {
    private final CurrentUserRepo currentUserRepository;

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    public GithubClient.AccessTokenProvider githubAccessTokenProvider() {
        return () -> currentUserRepository
                .get()
                .flatMap(user -> Mono.justOrEmpty(user.getGithubAccessToken()));
    }

    @Bean
    public GithubClient.LoginProvider githubLoginProvider() {
        return () -> currentUserRepository
                .get()
                .flatMap(user -> Mono.just(user.getGithubUser().getLogin()));
    }

    @Bean
    public RepositoryProvider repositoryManager(@Value("${accula.reposPath}") final String reposPath) {
        final File root = new File(reposPath);
        return new RepositoryManager(root);
    }

    @Bean
    public CloneDetector cloneDetector() {
        return new PrimitiveCloneDetector(5, 7);
    }
}
