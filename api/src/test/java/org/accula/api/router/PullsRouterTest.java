package org.accula.api.router;

import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.PullsHandler;
import org.accula.api.handler.dto.PullDto;
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
import java.util.Map;

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

        client.get().uri("/api/projects/{projectId}/pulls/{pullNumber}", STUB_PULL.getProjectId(), STUB_PULL.getNumber())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PullDto.class).isEqualTo(ModelToDtoConverter.convert(STUB_PULL, Collections.emptyList()));
    }

    @Test
    void testGetBadRequest() {
        client.get().uri("/api/projects/{projectId}/pulls/notANumber", STUB_PULL.getProjectId())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map.class).isEqualTo(Map.of("code", "BAD_REQUEST"));
    }

    @Test
    void testGetNotFound() {
        Mockito.when(repository.findByNumber(Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{projectId}/pulls/{pullNumber}", STUB_PULL.getProjectId(), STUB_PULL.getNumber())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Map.class).isEqualTo(Map.of("code", "NOT_FOUND"));
    }
}
