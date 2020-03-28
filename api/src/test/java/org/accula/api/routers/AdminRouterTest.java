package org.accula.api.routers;

import org.accula.api.config.SecurityConfig;
import org.accula.api.handlers.AdminHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@ContextConfiguration(classes = {SecurityConfig.class, AdminRouter.class, AdminHandler.class})
@DisplayName("Admin router tests")
public class AdminRouterTest {
    @Autowired
    private ApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    public void setUp() {
        client = WebTestClient
                .bindToApplicationContext(context)
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Test correct admin request with ADMIN role")
    public void testAdminRouterOk() {
        client.get()
                .uri("/admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("You are an admin now.");
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("Test invalid admin request with USER role")
    public void testAdminRouterNotLoggedIn() {
        client.get()
                .uri("/admin")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).isEqualTo("Access Denied");
    }
}
