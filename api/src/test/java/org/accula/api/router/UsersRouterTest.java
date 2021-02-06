package org.accula.api.router;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handler.UsersHandler;
import org.accula.api.handler.dto.UserDto;
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
@ContextConfiguration(classes = {UsersRouter.class, UsersHandler.class, ModelToDtoConverter.class})
public class UsersRouterTest {
    static final GithubUser GITHUB_USER = new GithubUser(1L, "login", "name", "ava", false);
    static final User STUB_USER = new User(1L, "token", GITHUB_USER);
    private static final ResponseUser RESPONSE_USER =
            new ResponseUser(STUB_USER.id(), GITHUB_USER.login(), GITHUB_USER.name());

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

    @SneakyThrows
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
    public void testGetUserByIdBadRequest() {
        client.get().uri("/api/users/notANumber")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testGetUserByIdNotFoundInRepo() {
        Mockito.when(repository.findById(Mockito.anyLong()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/users/{id}", RESPONSE_USER.id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @JsonInclude
    @Value
    private static class ResponseUser {
        Long id;
        String login;
        String name;
    }
}