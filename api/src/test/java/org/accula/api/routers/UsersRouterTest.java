package org.accula.api.routers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.accula.api.db.UserRepository;
import org.accula.api.db.dto.User;
import org.accula.api.handlers.UsersHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@Import({UsersRouter.class, UsersHandler.class, UserRepository.class})
public class UsersRouterTest {
    private static final User STUB_DTO_USER = new User(1L, "name", 123L, "login", null);
    private static final ResponseUser RESPONSE_USER =
            new ResponseUser(STUB_DTO_USER.getId(), STUB_DTO_USER.getGithubLogin(), STUB_DTO_USER.getName());

    @MockBean
    private UserRepository repository;
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
    public void tesGetUserByIdOk() {
        Mockito.when(repository.findById(RESPONSE_USER.id))
                .thenReturn(Mono.just(STUB_DTO_USER));

        client.get().uri("/users/{id}", RESPONSE_USER.id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResponseUser.class).isEqualTo(RESPONSE_USER);
    }

    @Test
    public void testGetUserByIdNotFoundInRepo() {
        Mockito.when(repository.findById(RESPONSE_USER.id))
                .thenReturn(Mono.empty());

        client.get().uri("/users/{id}", RESPONSE_USER.id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void testGetUserByIdNotFoundWrongId() {
        client.get().uri("/users/notANumber")
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
