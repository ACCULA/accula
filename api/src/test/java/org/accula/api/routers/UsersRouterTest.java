package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handler.UsersHandler;
import org.accula.api.handler.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.accula.api.util.TestData.lamtev;
import static org.accula.api.util.TestData.polis;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    UsersRouter.class,
    UsersHandler.class,
})
public class UsersRouterTest {
    private static final UserDto RESPONSE_USER_WITH_ROLE = ModelToDtoConverter.convertWithRole(polis);
    private static final UserDto RESPONSE_USER = ModelToDtoConverter.convert(polis);

    @MockBean
    private UserRepo userRepo;
    @MockBean
    private CurrentUserRepo currentUserRepo;

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
        when(userRepo.findById(anyLong()))
            .thenReturn(Mono.just(polis));
        when(currentUserRepo.get())
            .thenReturn(Mono.empty());

        client.get().uri("/api/users/{id}", RESPONSE_USER.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class).isEqualTo(RESPONSE_USER);
    }

    @SneakyThrows
    @Test
    public void testGetUserByIdOkWithRoleAsRoot() {
        when(userRepo.findById(anyLong()))
            .thenReturn(Mono.just(polis));
        when(currentUserRepo.get())
            .thenReturn(Mono.just(lamtev));

        client.get().uri("/api/users/{id}", RESPONSE_USER_WITH_ROLE.id())
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserDto.class).isEqualTo(RESPONSE_USER_WITH_ROLE);
    }

    @SneakyThrows
    @Test
    public void testGetUserByIdOkWithRoleAsCurrentUser() {
        when(userRepo.findById(anyLong()))
            .thenReturn(Mono.just(polis));
        when(currentUserRepo.get())
            .thenReturn(Mono.just(polis));

        client.get().uri("/api/users/{id}", RESPONSE_USER_WITH_ROLE.id())
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserDto.class).isEqualTo(RESPONSE_USER_WITH_ROLE);
    }

    @Test
    public void testGetUserByIdBadRequest() {
        client.get().uri("/api/users/notANumber")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testGetUserByIdNotFoundInRepo() {
        when(userRepo.findById(anyLong()))
                .thenReturn(Mono.empty());
        when(currentUserRepo.get())
            .thenReturn(Mono.empty());

        client.get().uri("/api/users/{id}", RESPONSE_USER.id())
                .exchange()
                .expectStatus().isNotFound();
    }
}
