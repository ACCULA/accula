package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.JGitCodeLoader;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.PrimitiveCloneDetector;
import org.accula.github.api.GithubClient;
import org.accula.github.api.GithubClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

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
    public GithubClient githubClient(final WebClient webClient) {
        return new GithubClientImpl(
                () -> currentUserRepo
                        .get()
                        .map(User::getGithubAccessToken),
                () -> currentUserRepo
                        .get()
                        .map(user -> user.getGithubUser().getLogin()),
                webClient
        );
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
