package org.accula.api.routers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootTest
@RunWith(SpringRunner.class)
public final class GreetingRouterTest {
    private static final String GREETING = "ACCULA is greeting you, Alice";
    private static final String ERROR = "Missing required query param \"name\"";

    @Autowired
    private RouterFunction<ServerResponse> greetingRoute;

    @Test
    public void testGreetingRouteOk() {
        WebTestClient
                .bindToRouterFunction(greetingRoute)
                .build()

                .get()
                .uri("/greet?name=Alice")
                .exchange()

                .expectStatus()
                .isOk()

                .expectBody(String.class)
                .isEqualTo(GREETING);
    }

    @Test
    public void testGreetingRouteBadRequest() {
        WebTestClient
                .bindToRouterFunction(greetingRoute)
                .build()

                .get()
                .uri("/greet?name=")
                .exchange()

                .expectStatus()
                .isBadRequest()

                .expectBody(String.class)
                .isEqualTo(ERROR);
    }
}
