package org.accula.api.routers;

import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handler.AppHandler;
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.dto.AppSettingsDto;
import org.accula.api.handler.dto.validation.InputDtoValidator;
import org.accula.api.handler.exception.HandlerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.accula.api.db.model.User.Role.ADMIN;
import static org.accula.api.db.model.User.Role.ROOT;
import static org.accula.api.util.ApiErrors.toApiError;
import static org.accula.api.util.TestData.admin;
import static org.accula.api.util.TestData.lamtev;
import static org.accula.api.util.TestData.user;
import static org.accula.api.util.TestData.user1;
import static org.accula.api.util.TestData.user2;
import static org.accula.api.util.TestData.vaddya;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    AppHandler.class,
    AppRouter.class,
    InputDtoValidator.class,
})
class AppRouterTest {
    @Autowired
    RouterFunction<ServerResponse> appRoute;

    WebTestClient client;

    @MockBean
    CurrentUserRepo currentUser;
    @MockBean
    UserRepo userRepo;

    @BeforeEach
    void setUp() {
        client = WebTestClient
            .bindToRouterFunction(appRoute)
            .build();
    }

    @Test
    void testAppSettingsUrl() {
        client
            .get()
            .uri("/api/app/settingsUrl")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class).value(response -> {
                final var settingsUrl = (String) response.get("settingsUrl");
                assertNotNull(settingsUrl);
                assertTrue(settingsUrl.contains("https://github.com/settings/connections/applications/"));
            });
    }

    @Test
    void testGetSettings() {
        when(currentUser.get())
            .thenReturn(Mono.just(lamtev));
        when(userRepo.findAll())
            .thenReturn(Flux.just(user, vaddya, admin, user1, user2, lamtev));

        client
            .get()
            .uri("/api/app/settings")
            .exchange()
            .expectStatus().isOk()
            .expectBody(AppSettingsDto.class).value(settings -> {
                assertEquals(Stream.of(user, admin, user1, user2).map(ModelToDtoConverter::convertWithRole).toList(), settings.users());
                assertEquals(Stream.of(vaddya, lamtev).map(ModelToDtoConverter::convertWithRole).toList(), settings.roots());
                assertEquals(List.of(admin.id()), settings.adminIds());
            });
    }

    @Test
    void testUpdateSettings() {
        when(currentUser.get())
            .thenReturn(Mono.just(lamtev));

        when(userRepo.findAll())
            .thenReturn(Flux.just(user, vaddya, admin, user1, user2, lamtev));

        when(userRepo.setAdminRole(anyCollection()))
            .thenReturn(Mono.just(List.of(user, vaddya, admin, user1.withRole(ADMIN), user2, lamtev)));

        client
            .put()
            .uri("/api/app/settings")
            .contentType(APPLICATION_JSON)
            .bodyValue(new AppSettingsDto(null, null, Stream.of(user1).map(User::id).toList()))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AppSettingsDto.class).value(settings -> {
                assertEquals(Stream.of(user, admin, user1.withRole(ADMIN), user2).map(ModelToDtoConverter::convertWithRole).toList(), settings.users());
                assertEquals(Stream.of(vaddya, lamtev).map(ModelToDtoConverter::convertWithRole).toList(), settings.roots());
                assertEquals(List.of(admin.id(), user1.id()), settings.adminIds());
            });
    }

    @Test
    void testUpdateSettingsNoRootRole() {
        when(currentUser.get())
            .thenReturn(Mono.just(admin));

        client
            .put()
            .uri("/api/app/settings")
            .contentType(APPLICATION_JSON)
            .bodyValue(Map.of())
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(ApiError.class).isEqualTo(toApiError(HandlerException.atLeastRoleRequired(ROOT)));
    }
}
