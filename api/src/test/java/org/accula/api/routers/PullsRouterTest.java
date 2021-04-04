package org.accula.api.routers;

import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.PullsHandler;
import org.accula.api.handler.dto.PullDto;
import org.accula.api.handler.dto.ShortPullDto;
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

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {PullsRouter.class, PullsHandler.class, ModelToDtoConverter.class})
class PullsRouterTest {
    static final Pull STUB_PULL = ProjectsRouterTest.PULL;
    @MockBean
    PullRepo repository;
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
        Mockito.when(repository.findByNumber(Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.just(STUB_PULL));

        Mockito.when(repository.findPrevious(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyLong()))
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
                .expectBody(Void.class);
    }

    @Test
    void testGetNotFound() {
        Mockito.when(repository.findByNumber(Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{projectId}/pulls/{pullNumber}", 1L, STUB_PULL.number())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Void.class);
    }

    @Test
    void testGetManyOk() {
        Mockito.when(repository.findByProjectId(Mockito.anyLong()))
                .thenReturn(Flux.just(STUB_PULL));

        client.get().uri("/api/projects/{projectId}/pulls", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ShortPullDto[].class).isEqualTo(ModelToDtoConverter.convertShort(List.of(STUB_PULL)).toArray(new ShortPullDto[0]));
    }

    @Test
    void testGetManyEmpty() {
        Mockito.when(repository.findByProjectId(Mockito.anyLong()))
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
}
