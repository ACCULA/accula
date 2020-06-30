package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.JGitCodeLoader;
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
    private final CurrentUserRepo currentUserRepo;

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    public GithubClient.AccessTokenProvider githubAccessTokenProvider() {
        return () -> currentUserRepo
                .get()
                .flatMap(user -> Mono.just(user.getGithubAccessToken()));
    }

    @Bean
    public GithubClient.LoginProvider githubLoginProvider() {
        return () -> currentUserRepo
                .get()
                .flatMap(user -> Mono.just(user.getGithubUser().getLogin()));
    }

    @Bean
    public CodeLoader codeLoader(@Value("${accula.reposPath}") final String reposPath) {
        return new JGitCodeLoader(new File(reposPath));
    }

    @Bean
    public CloneDetector cloneDetector() {
        return new PrimitiveCloneDetector(3, 8);
    }
}
