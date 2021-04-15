package org.accula.api.routers;

import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.PullsHandler;
import org.accula.api.handler.dto.PullDto;
import org.accula.api.handler.dto.ShortPullDto;
import org.hamcrest.Matchers;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    PullsRouter.class,
    PullsHandler.class,
})
class PullsRouterTest {
    static final Pull STUB_PULL = ProjectsRouterTest.PULL;
    @MockBean
    PullRepo repository;
    @MockBean
    CurrentUserRepo currentUserRepo;
    @Autowired
    RouterFunction<ServerResponse> pullsRoute;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient
                .bindToRouterFunction(pullsRoute)
                .build();
    }

    @Test
    void testGetOk() {
        when(repository.findByNumber(anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.just(STUB_PULL));

        when(repository.findPrevious(anyLong(), Mockito.anyInt(), anyLong()))
                .thenReturn(Flux.fromIterable(Collections.emptyList()));

        client.get().uri("/api/projects/{projectId}/pulls/{pullNumber}", 1L, STUB_PULL.number())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PullDto.class).isEqualTo(ModelToDtoConverter.convert(STUB_PULL, Collections.emptyList()));
    }

    @Test
    void testGetBadRequest() {
        client.get().uri("/api/projects/{projectId}/pulls/notANumber", 1L)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testGetNotFound() {
        when(repository.findByNumber(anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{projectId}/pulls/{pullNumber}", 1L, STUB_PULL.number())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testGetManyOk() {
        when(repository.findByProjectId(anyLong()))
                .thenReturn(Flux.just(STUB_PULL));

        client.get().uri("/api/projects/{projectId}/pulls", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ShortPullDto[].class).isEqualTo(ModelToDtoConverter.convertShort(List.of(STUB_PULL)).toArray(new ShortPullDto[0]));
    }

    @Test
    void testGetManyEmpty() {
        when(repository.findByProjectId(anyLong()))
                .thenReturn(Flux.empty());

        client.get().uri("/api/projects/{projectId}/pulls", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class).isEqualTo(Collections.emptyList());
    }

    @Test
    void testGetManyBadRequest() {
        client.get().uri("/api/projects/notANumber/pulls")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testGetManyAssignedToMeAuthorized() {
        final var pull = STUB_PULL.withAssignees(List.of(UsersRouterTest.STUB_USER.githubUser()));
        when(repository.findByProjectId(anyLong()))
            .thenReturn(Flux.just(pull));
        when(currentUserRepo.get(any()))
            .thenReturn(Mono.just(UsersRouterTest.STUB_USER.githubUser()));

        client.get().uri("/api/projects/{projectId}/pulls?assignedToMe", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ShortPullDto[].class).isEqualTo(ModelToDtoConverter.convertShort(List.of(pull)).toArray(new ShortPullDto[0]));
    }

    @Test
    void testGetManyAssignedToMeAuthorizedNotMatches() {
        when(repository.findByProjectId(anyLong()))
            .thenReturn(Flux.just(STUB_PULL));
        when(currentUserRepo.get(any()))
            .thenReturn(Mono.just(UsersRouterTest.STUB_USER.githubUser()));

        client.get().uri("/api/projects/{projectId}/pulls?assignedToMe", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ShortPullDto[].class).value(pulls -> assertEquals(0, pulls.length));
    }

    @Test
    void testGetManyAssignedToMeNotAuthorized() {
        when(repository.findByProjectId(anyLong()))
            .thenReturn(Flux.just(STUB_PULL));
        when(currentUserRepo.get(any()))
            .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{projectId}/pulls?assignedToMe", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ShortPullDto[].class).value(pulls -> assertEquals(0, pulls.length));
    }
}
