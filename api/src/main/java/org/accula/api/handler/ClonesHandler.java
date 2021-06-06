package org.accula.api.handler;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.SnippetMarker;
import org.accula.api.code.lines.LineRange;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.dto.CloneDto;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.PathVariableExtractor;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.util.Checks;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Component
@RequiredArgsConstructor
public final class ClonesHandler {
    private static final Comparator<Clone> DESCENDING_BY_LINE_COUNT = Comparator
        .comparing((Clone clone) -> Math.max(clone.target().lineCount(), clone.source().lineCount()))
        .reversed();
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CurrentUserRepo currentUserRepo;
    private final ProjectRepo projectRepo;
    private final CloneDetectionService cloneDetectionService;
    private final CodeLoader codeLoader;

    //TODO: Return only clones missing in the previous pulls
    public Mono<ServerResponse> getPullClones(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = PathVariableExtractor.projectId(request);
                    final var pullNumber = PathVariableExtractor.pullNumber(request);
                    return getPullClones(projectId, pullNumber);
                })
                .onErrorResume(NumberFormatException.class, Lambda.expandingWithArg(Responses::badRequest))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> refreshClones(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = PathVariableExtractor.projectId(request);
                    final var pullNumber = PathVariableExtractor.pullNumber(request);

                    final var clones = havingAdminPermissionAtProject(
                        projectId,
                        cloneRepo
                            .deleteByPullNumber(projectId, pullNumber)
                            .then(pullRepo.findByNumber(projectId, pullNumber))
                            .flatMapMany(cloneDetectionService::readClonesAndSaveToDb)
                        )
                        .cache();
                    return toResponse(clones, projectId)
                            .switchIfEmpty(Responses.forbidden());
                })
                .onErrorResume(NumberFormatException.class, Lambda.expandingWithArg(Responses::badRequest));
    }

    public Mono<ServerResponse> topPlagiarists(final ServerRequest request) {
        return Mono
            .defer(() -> cloneRepo
                .topPlagiarists(PathVariableExtractor.projectId(request))
                .map(ModelToDtoConverter::convert)
                .collectList()
                .flatMap(Responses::ok))
            .onErrorResume(NumberFormatException.class, Lambda.expandingWithArg(Responses::badRequest));
    }

    private <T> Flux<T> havingAdminPermissionAtProject(final Long projectId, final Flux<T> action) {
        return currentUserRepo
                .get(User::id)
                .filterWhen(currentUserId -> projectRepo.hasAdmin(projectId, currentUserId))
                .switchIfEmpty(Mono.error(Http4xxException::forbidden))
                .flatMapMany(currentUserId -> action);
    }

    private Mono<ServerResponse> getPullClones(final Long projectId, final Integer pullNumber) {
        final var clones = cloneRepo
                .findByPullNumber(projectId, pullNumber)
                .cache();
        return toResponse(clones, projectId);
    }

    private Mono<ServerResponse> toResponse(final Flux<Clone> clones, final Long projectId) {
        final var cloneSnippets = clones
                .flatMap(clone -> Flux.just(clone.target(), clone.source()))
                .cache();

        final var fileSnippets = cloneSnippets
                .groupBy(Clone.Snippet::snapshot)
                .flatMap(snippetFlux -> snippetFlux
                        .collectList()
                        .flatMapMany(snippets -> codeLoader
                                .loadSnippets(
                                        snippetFlux.key(),
                                        snippets.stream()
                                                .map(s -> SnippetMarker.of(s.file(), LineRange.of(s.fromLine(), s.toLine())))
                                                .toList()
                                )
                                .zipWithIterable(snippets)
                        )
                )
                .collectMap(Tuple2::getT2, Tuple2::getT1);

        final var pulls = cloneSnippets
            .map(snippet -> snippet.snapshot().pullInfo().id())
            .collectList()
            .flatMapMany(pullRepo::findById)
            .collectMap(Pull::id);

        final var responseClones = Mono
                .zip(clones.collectSortedList(DESCENDING_BY_LINE_COUNT), fileSnippets, pulls, Mono.just(projectId))
                .flatMapMany(TupleUtils.function(ClonesHandler::convert));

        return responseClones
            .collectList()
            .flatMap(Responses::ok);
    }

    private static Flux<CloneDto> convert(final List<Clone> clones,
                                          final Map<Clone.Snippet, FileEntity<Snapshot>> files,
                                          final Map<Long, Pull> pulls,
                                          final Long projectId) {
        Preconditions.checkArgument(clones.stream().flatMap(c -> Stream.of(c.target(), c.source())).distinct().count() == files.size());
        return Flux.push(sink -> {
            for (final var clone : clones) {
                final var dto = ModelToDtoConverter.convert(
                        clone,
                        projectId,
                        Checks.notNull(files.get(clone.target()), "Clone target file content"),
                        Checks.notNull(files.get(clone.source()), "Clone source file content"),
                        Checks.notNull(pulls.get(clone.target().snapshot().pullInfo().id()), "Clone target pull"),
                        Checks.notNull(pulls.get(clone.source().snapshot().pullInfo().id()), "Clone source pull")
                );
                sink.next(dto);
            }
            sink.complete();
        });
    }
}
