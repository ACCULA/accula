package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.auth.jwt.JwtAuthFilter;
import org.accula.auth.jwt.JwtAuthenticationConverter;
import org.accula.auth.jwt.JwtRefreshFilter;
import org.accula.auth.oauth2.OAuth2LoginFailureHandler;
import org.accula.auth.oauth2.OAuth2LoginSuccessHandler;
import org.accula.auth.jwt.crypto.Jwt;
import org.accula.auth.jwt.crypto.EcKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.server.WebFilter;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.HTTP_HEADERS_WRITER;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

/**
 * @author Anton Lamtev
 */
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final ReactiveClientRegistrationRepository clientRegistrations;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http,
                                                  final WebFilter authenticationFilter,
                                                  final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {
        return http
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
                .headers().disable()

                //https://github.com/spring-projects/spring-security/issues/6552#issuecomment-515571416
                .requestCache(cache -> cache
                        .requestCache(NoOpServerRequestCache.getInstance()))

                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(FORBIDDEN)))

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/greet").authenticated()
                        .anyExchange().permitAll())

                .addFilterAt(new JwtRefreshFilter(pathMatchers(POST, "/refreshToken")), HTTP_HEADERS_WRITER)
                .addFilterAt(authenticationFilter, AUTHENTICATION)

                .oauth2Login(oauth2 -> oauth2
                        .authenticationMatcher(pathMatchers(GET, "/login/{registrationId}/callback"))
                        .authorizationRequestResolver(new DefaultServerOAuth2AuthorizationRequestResolver(
                                clientRegistrations, pathMatchers(GET, "/login/{registrationId}")))
                        .authenticationSuccessHandler(oauth2LoginSuccessHandler)
                        .authenticationFailureHandler(new OAuth2LoginFailureHandler())
                )
                .build();
    }

    @Bean
    ECPublicKey publicKey(@Value("${accula.jwt.signature.public}") final String name) throws URISyntaxException {
        final var resource = requireNonNull(getClass().getClassLoader().getResource(name));

        return EcKeys.publicKey(Path.of(resource.toURI()));
    }

    @Bean
    ECPrivateKey privateKey(@Value("${accula.jwt.signature.private}") final String name) throws URISyntaxException {
        final var resource = requireNonNull(getClass().getClassLoader().getResource(name));

        return EcKeys.privateKey(Path.of(resource.toURI()));
    }

    @Bean
    Jwt jwt(final ECPrivateKey privateEcKey,
            final ECPublicKey publicEcKey,
            @Value("${accula.jwt.issuer}") final String issuer,
            @Value("${accula.jwt.expiresInMins}") final int expiresInMins) {

        return new Jwt(privateEcKey, publicEcKey, issuer, expiresInMins);
    }

    @Bean
    OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(final Jwt jwt) {
        return new OAuth2LoginSuccessHandler(jwt);
    }

    @Bean
    ServerAuthenticationConverter authenticationConverter(final Jwt jwt) {
        return new JwtAuthenticationConverter(jwt);
    }

    @Bean
    WebFilter authenticationFilter(final ServerAuthenticationConverter authenticationConverter) {
        final var filter = new JwtAuthFilter();
        filter.setServerAuthenticationConverter(authenticationConverter);

        return filter;
    }
}
