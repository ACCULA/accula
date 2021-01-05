package org.accula.api.config;

import lombok.RequiredArgsConstructor;

import org.accula.api.code.CodeLoader;
import org.accula.api.code.GitCodeLoader;
import org.accula.api.code.git.Git;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.github.api.GithubClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        return () -> currentUserRepo.get(User::getGithubAccessToken);
    }

    @Bean
    public GithubClient.LoginProvider githubLoginProvider() {
        return () -> currentUserRepo.get(user -> user.getGithubUser().getLogin());
    }

    @Bean
    public Git git(@Value("${accula.reposPath}") final Path reposPath) throws IOException {
        if (!Files.exists(reposPath)) {
            Files.createDirectory(reposPath);
        }
        final var availableProcessors = Runtime.getRuntime().availableProcessors();
        final var executor = new ThreadPoolExecutor(
                availableProcessors,
                availableProcessors * 10,
                1L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(availableProcessors * 50)
        );

        return new Git(reposPath, executor);
    }

    @Bean
    public CodeLoader codeLoader(final Git git) {
        return new GitCodeLoader(git);
    }
}
