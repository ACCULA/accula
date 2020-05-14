package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.api.db.CurrentUserRepository;
import org.accula.api.github.api.GithubClient;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@SpringBootConfiguration
@RequiredArgsConstructor
public class WebConfig implements WebFluxConfigurer {
    private final CurrentUserRepository currentUserRepository;

    @Override
    public void addCorsMappings(final CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("*")
                .exposedHeaders("Access-Control-Allow-Origin")
                .allowCredentials(true);
    }

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
                .flatMap(user -> Mono.just(user.getGithubLogin()));
    }
}
