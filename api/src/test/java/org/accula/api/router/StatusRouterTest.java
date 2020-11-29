package org.accula.api.router;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@WebFluxTest
@ContextConfiguration(classes = {StatusRouter.class})
public class StatusRouterTest {
    private static final String STATUS = "{\"status\":\"ONLINE\"}";

    @Autowired
    private RouterFunction<ServerResponse> statusRoute;

    @Test
    public void testStatusRoute() {
        WebTestClient
                .bindToRouterFunction(statusRoute)
                .build()
                .get().uri("/api/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(STATUS);
    }
}
