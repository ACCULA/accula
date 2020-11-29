package org.accula.api.handler.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
public class LoggingWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() ->
                Mono.fromRunnable(() -> {
                    final var request = exchange.getRequest();
                    if (log.isInfoEnabled()) {
                        log.info("HTTP {} {}: {}", request.getMethod(), request.getPath(), exchange.getResponse().getStatusCode());
                    }
                }));
        return chain.filter(exchange);
    }
}
