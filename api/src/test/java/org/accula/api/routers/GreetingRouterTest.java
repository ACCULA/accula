package org.accula.api.routers;

import org.accula.api.handlers.GreetingHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@WebFluxTest
@ContextConfiguration(classes = {GreetingRouter.class, GreetingHandler.class})
public final class GreetingRouterTest {
    private static final String GREETING = "ACCULA is greeting you, Alice";
    private static final String ERROR = "Missing required query param \"name\"";

    @Autowired
    private RouterFunction<ServerResponse> greetingRoute;
    private WebTestClient client;

    @BeforeEach
    public void setUp() {
        client = WebTestClient
                .bindToRouterFunction(greetingRoute)
                .build();
    }

    @Test
    public void testGreetingRouteOk() {
        client.get().uri("/greet?name=Alice")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(GREETING);
    }

    @Test
    public void testGreetingRouteBadRequest() {
        client.get().uri("/greet?name=")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(ERROR);
    }
}
