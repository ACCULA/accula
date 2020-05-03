package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.JwtAccessTokenResponseProducer;
import org.accula.api.auth.jwt.JwtAuthFilter;
import org.accula.api.auth.jwt.JwtAuthenticationConverter;
import org.accula.api.auth.jwt.JwtRefreshFilter;
import org.accula.api.auth.jwt.crypto.EcKeys;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.oauth2.OAuth2LoginFailureHandler;
import org.accula.api.auth.oauth2.OAuth2LoginSuccessHandler;
import org.accula.api.db.RefreshTokenRepository;
import org.accula.api.db.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

/**
 * @author Anton Lamtev
 */
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final ReactiveClientRegistrationRepository clientRegistrations;
    private final JwtProperties jwtProperties;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http,
                                                         final WebFilter authenticationFilter,
                                                         final WebFilter jwtRefreshFilter,
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

                .addFilterBefore(jwtRefreshFilter, AUTHENTICATION)
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
    public ECPublicKey publicKey() throws URISyntaxException {
        final var resource = requireNonNull(getClass().getClassLoader().getResource(jwtProperties.getSignature().getPublicKey()));

        return EcKeys.publicKey(Path.of(resource.toURI()));
    }

    @Bean
    public ECPrivateKey privateKey() throws URISyntaxException {
        final var resource = requireNonNull(getClass().getClassLoader().getResource(jwtProperties.getSignature().getPrivateKey()));

        return EcKeys.privateKey(Path.of(resource.toURI()));
    }

    @Bean
    public Jwt jwt(final ECPrivateKey privateEcKey, final ECPublicKey publicEcKey) {
        return new Jwt(privateEcKey, publicEcKey, jwtProperties.getIssuer());
    }

    @Bean
    public JwtAccessTokenResponseProducer jwtAccessTokenResponseProducer(final Jwt jwt) {
        return new JwtAccessTokenResponseProducer(
                jwt,
                jwtProperties.getExpiresIn().getAccess(),
                jwtProperties.getExpiresIn().getRefresh()
        );
    }

    @Bean
    public OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(
            final JwtAccessTokenResponseProducer jwtAccessTokenResponseProducer,
            final Jwt jwt,
            final ReactiveOAuth2AuthorizedClientService authorizedClientService,
            final UserRepository users,
            final RefreshTokenRepository refreshTokens) {

        return new OAuth2LoginSuccessHandler(
                jwtAccessTokenResponseProducer,
                jwt,
                jwtProperties.getExpiresIn().getRefresh(),
                authorizedClientService,
                users,
                refreshTokens
        );
    }

    @Bean
    public ServerAuthenticationConverter authenticationConverter(final Jwt jwt) {
        return new JwtAuthenticationConverter(jwt);
    }

    @Bean
    public WebFilter authenticationFilter(final ServerAuthenticationConverter authenticationConverter) {
        final var filter = new JwtAuthFilter();
        filter.setServerAuthenticationConverter(authenticationConverter);

        return filter;
    }

    @Bean
    public WebFilter jwtRefreshFilter(final JwtAccessTokenResponseProducer jwtAccessTokenResponseProducer,
                                      final Jwt jwt,
                                      final RefreshTokenRepository refreshTokens) {
        return new JwtRefreshFilter(
                pathMatchers(GET, "/refreshToken"),
                jwtAccessTokenResponseProducer,
                jwt,
                jwtProperties.getExpiresIn().getRefresh(),
                refreshTokens
        );
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        final var corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addExposedHeader(HttpHeaders.SET_COOKIE);
        final var corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(corsConfigurationSource);
    }
}
