package org.accula.api.config;

import lombok.RequiredArgsConstructor;
import org.accula.api.auth.jwt.JwtAccessTokenResponseProducer;
import org.accula.api.auth.jwt.JwtAuthFilter;
import org.accula.api.auth.jwt.JwtAuthenticationConverter;
import org.accula.api.auth.jwt.crypto.EcKeys;
import org.accula.api.auth.jwt.crypto.Jwt;
import org.accula.api.auth.jwt.refresh.JwtRefreshFilter;
import org.accula.api.auth.oauth2.OAuth2LoginFailureHandler;
import org.accula.api.auth.oauth2.OAuth2LoginSuccessHandler;
import org.accula.api.auth.util.CookieRefreshTokenHelper;
import org.accula.api.db.repo.RefreshTokenRepo;
import org.accula.api.db.repo.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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

import java.util.Collections;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.CORS;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final ReactiveClientRegistrationRepository clientRegistrations;
    private final JwtProperties jwtProperties;
    @Value("${accula.cluster.webUrl}")
    private String webUrl;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http,
                                                         final CorsWebFilter corsWebFilter,
                                                         final WebFilter authenticationFilter,
                                                         final WebFilter jwtRefreshFilter,
                                                         final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {
        return http
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
                .headers().disable()
                .csrf().disable()

                //https://github.com/spring-projects/spring-security/issues/6552#issuecomment-515571416
                .requestCache(cache -> cache
                        .requestCache(NoOpServerRequestCache.getInstance()))

                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(FORBIDDEN)))

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(GET, "/api/projects/{id}/githubAdmins").authenticated()
                        .pathMatchers(GET, "/api/projects/{id}/headFiles").authenticated()
                        .pathMatchers(GET, "/api/projects/{id}/conf").authenticated()
                        .pathMatchers(GET, "/api/projects/**").permitAll()
                        .pathMatchers("/api/projects/**").authenticated()
                        .anyExchange().permitAll())

                .addFilterAt(corsWebFilter, CORS)
                .addFilterBefore(jwtRefreshFilter, AUTHENTICATION)
                .addFilterAt(authenticationFilter, AUTHENTICATION)

                .oauth2Login(oauth2 -> oauth2
                        .authenticationMatcher(pathMatchers(GET, "/api/login/{registrationId}/callback"))
                        .authorizationRequestResolver(new DefaultServerOAuth2AuthorizationRequestResolver(
                                clientRegistrations, pathMatchers(GET, "/api/login/{registrationId}")))
                        .authenticationSuccessHandler(oauth2LoginSuccessHandler)
                        .authenticationFailureHandler(new OAuth2LoginFailureHandler())
                )
                .build();
    }

    @Bean
    public Jwt jwt() {
        return new Jwt(
                EcKeys.privateKey(jwtProperties.getSignature().getPrivateKey()),
                EcKeys.publicKey(jwtProperties.getSignature().getPublicKey()),
                jwtProperties.getIssuer()
        );
    }

    @Bean
    public CookieRefreshTokenHelper cookieRefreshTokenHelper() {
        return new CookieRefreshTokenHelper(jwtProperties.getRefreshTokenEndpointPath());
    }

    @Bean
    public JwtAccessTokenResponseProducer accessTokenResponseProducer(
            final Jwt jwt,
            final CookieRefreshTokenHelper cookieRefreshTokenHelper) {
        return new JwtAccessTokenResponseProducer(
                jwt,
                jwtProperties.getExpiresIn().getAccess(),
                jwtProperties.getExpiresIn().getRefresh(),
                cookieRefreshTokenHelper,
                webUrl
        );
    }

    @Bean
    public OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(
            final JwtAccessTokenResponseProducer accessTokenResponseProducer,
            final Jwt jwt,
            final ReactiveOAuth2AuthorizedClientService authorizedClientService,
            final UserRepo users,
            final RefreshTokenRepo refreshTokens) {
        return new OAuth2LoginSuccessHandler(
                accessTokenResponseProducer,
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
    public CorsWebFilter corsWebFilter(@Value("${accula.cluster.webUrl}") final String webUrl) {
        final var corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Collections.singletonList(webUrl));
        corsConfig.addAllowedMethod(CorsConfiguration.ALL);
        corsConfig.addAllowedHeader(CorsConfiguration.ALL);
        corsConfig.setAllowCredentials(true);

        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
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
                                      final RefreshTokenRepo refreshTokens,
                                      final CookieRefreshTokenHelper cookieRefreshTokenHelper) {
        return new JwtRefreshFilter(
                pathMatchers(jwtProperties.getRefreshTokenEndpointPath()),
                cookieRefreshTokenHelper,
                jwtAccessTokenResponseProducer,
                jwt,
                jwtProperties.getExpiresIn().getRefresh(),
                refreshTokens,
                webUrl
        );
    }
}
