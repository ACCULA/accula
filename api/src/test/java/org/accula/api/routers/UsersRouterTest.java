package org.accula.api.routers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handlers.UsersHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {UsersRouter.class, UsersHandler.class})
public class UsersRouterTest {
    private static final GithubUser GITHUB_USER = new GithubUser(1L, "login", "name", "ava", false);
    private static final User STUB_USER = new User(1L, GITHUB_USER, "token");
    private static final ResponseUser RESPONSE_USER =
            new ResponseUser(STUB_USER.getId(), GITHUB_USER.getLogin(), GITHUB_USER.getName());

    @MockBean
    private UserRepo repository;
    @Autowired
    private RouterFunction<ServerResponse> usersRoute;
    private WebTestClient client;

    @BeforeEach
    public void setUp() {
        client = WebTestClient
                .bindToRouterFunction(usersRoute)
                .build();
    }

    @Test
    public void testGetUserByIdOk() {
        Mockito.when(repository.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(STUB_USER));

        client.get().uri("/api/users/{id}", RESPONSE_USER.id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResponseUser.class).isEqualTo(RESPONSE_USER);
    }

    @Test
    public void testGetUserByIdNotFoundInRepo() {
        Mockito.when(repository.findById(Mockito.anyLong()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/users/{id}", RESPONSE_USER.id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void testGetUserByIdNotFoundWrongId() {
        client.get().uri("/api/users/notANumber")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ResponseUser {
        Long id;
        String login;
        String name;
    }
}
