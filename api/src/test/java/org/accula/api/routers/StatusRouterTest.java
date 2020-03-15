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
