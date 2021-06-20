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
import org.accula.api.util.Strings;
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

import static org.accula.api.db.model.User.Role.*;
import static org.accula.api.routers.ProjectsRouterTest.CURRENT_USER;
import static org.accula.api.routers.ProjectsRouterTest.CURRENT_USER_ADMIN;
import static org.accula.api.routers.ProjectsRouterTest.USER_2;
import static org.accula.api.routers.ProjectsRouterTest.USER_3;
import static org.accula.api.routers.ProjectsRouterTest.USER_4;
import static org.accula.api.util.ApiErrors.toApiError;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                assertDoesNotThrow(() -> {
                    final var id = Strings.suffixAfterPrefix(settingsUrl, "https://github.com/settings/connections/applications/");
                    assertNotNull(id);
                });
            });
    }

    @Test
    void testGetSettings() {
        when(currentUser.get())
            .thenReturn(Mono.just(CURRENT_USER));
        when(userRepo.findAll())
            .thenReturn(Flux.just(USER_2, CURRENT_USER_ADMIN, USER_3, USER_4, CURRENT_USER));

        client
            .get()
            .uri("/api/app/settings")
            .exchange()
            .expectStatus().isOk()
            .expectBody(AppSettingsDto.class).value(settings -> {
                assertEquals(Stream.of(USER_2, USER_3, USER_4).map(ModelToDtoConverter::convert).toList(), settings.users());
                assertEquals(List.of(CURRENT_USER_ADMIN.id()), settings.admins());
                assertEquals(List.of(CURRENT_USER.id()), settings.roots());
            });
    }

    @Test
    void testUpdateSettings() {
        when(currentUser.get())
            .thenReturn(Mono.just(CURRENT_USER));

        when(userRepo.findAll())
            .thenReturn(Flux.just(USER_2, CURRENT_USER_ADMIN, USER_3, USER_4, CURRENT_USER));

        when(userRepo.setAdminRole(anyCollection()))
            .thenReturn(Mono.just(List.of(USER_2, CURRENT_USER_ADMIN, USER_3.withRole(ADMIN), USER_4, CURRENT_USER)));

        client
            .put()
            .uri("/api/app/settings")
            .contentType(APPLICATION_JSON)
            .bodyValue(new AppSettingsDto(null, null, Stream.of(USER_3).map(User::id).toList()))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AppSettingsDto.class).value(settings -> {
                assertEquals(Stream.of(USER_2, USER_3.withRole(ADMIN), USER_4).map(ModelToDtoConverter::convert).toList(), settings.users());
                assertEquals(List.of(CURRENT_USER_ADMIN.id(), USER_3.id()), settings.admins());
                assertEquals(List.of(CURRENT_USER.id()), settings.roots());
            });
    }

    @Test
    void testUpdateSettingsNoRootRole() {
        when(currentUser.get())
            .thenReturn(Mono.just(CURRENT_USER_ADMIN));

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
