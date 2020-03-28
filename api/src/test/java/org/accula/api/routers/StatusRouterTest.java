package org.accula.api.routers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@WebFluxTest
@Import(StatusRouter.class)
public class StatusRouterTest {
    private static final String STATUS = "{\"status\":\"ONLINE\"}";

    @Autowired
    private RouterFunction<ServerResponse> statusRoute;

    @Test
    public void testStatusRoute() {
        WebTestClient
                .bindToRouterFunction(statusRoute)
                .build()
                .get().uri("/status").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(STATUS);
    }
}
