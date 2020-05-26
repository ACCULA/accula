package org.accula.api.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author Vadim Dyachkov
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CodeClientImpl implements CodeClient {
    public static final Exception REPO_NOT_FOUND = new Exception();
    public static final Exception FILE_NOT_FOUND = new Exception();
    public static final Exception CUT_ERROR = new Exception();
    public static final Exception RANGE_ERROR = new Exception();

    private final Scheduler scheduler = Schedulers.boundedElastic();

    private final RepositoryManager repositoryManager;

    @Override
    public Flux<FileEntity> getFiles(final CommitMarker marker) {
        return getFiles(marker, FileFilter.ALL);
    }

    @Override
    public Flux<FileEntity> getFiles(
            final CommitMarker marker,
            final FileFilter filter) {
        return repositoryManager.getRepository(marker.getOwner(), marker.getRepo())
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMapMany(rep -> Flux.fromIterable(getObjectLoaders(rep, marker.getSha())))
                .filter(tuple -> filter.test(tuple.getT1()))
                .map(tuple -> new FileEntity(tuple.getT1(), getFileContent(tuple.getT2())))
                .switchIfEmpty(Mono.error(CUT_ERROR))
                .subscribeOn(scheduler);
    }

    @Override
    public Mono<String> getFile(
            final CommitMarker marker,
            final String fileName) {
        return repositoryManager.getRepository(marker.getOwner(), marker.getRepo())
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMap(rep -> Mono.justOrEmpty(getObjectLoader(rep, marker.getSha(), fileName)))
                .switchIfEmpty(Mono.error(FILE_NOT_FOUND))
                .map(this::getFileContent)
                .switchIfEmpty(Mono.error(CUT_ERROR))
                .subscribeOn(scheduler);
    }

    @Override
    public Mono<String> getFileSnippet(
            final CommitMarker marker,
            final String fileName,
            final int fromLine,
            final int toLine) {
        if (fromLine > toLine) {
            return Mono.error(RANGE_ERROR);
        }
        return repositoryManager.getRepository(marker.getOwner(), marker.getRepo())
                .switchIfEmpty(Mono.error(REPO_NOT_FOUND))
                .flatMap(rep -> Mono.justOrEmpty(getObjectLoader(rep, marker.getSha(), fileName)))
                .switchIfEmpty(Mono.error(FILE_NOT_FOUND))
                .map(loader -> cutFileContent(loader, fromLine, toLine))
                .switchIfEmpty(Mono.error(CUT_ERROR))
                .subscribeOn(scheduler);
    }

    @SneakyThrows
    @Nullable
    private ObjectLoader getObjectLoader(
            final Repository repository,
            final String sha,
            final String file) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree revTree = commit.getTree();
        final TreeWalk treeWalk = TreeWalk.forPath(reader, file, revTree);
        if (treeWalk == null) {
            log.error("Cannot find file {} in repo {}, sha={}", file, repository.getWorkTree(), sha);
            return null;
        }
        return reader.open(treeWalk.getObjectId(0));
    }

    @SneakyThrows
    private List<Tuple2<String, ObjectLoader>> getObjectLoaders(
            final Repository repository,
            final String sha) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree revTree = commit.getTree();

        final List<Tuple2<String, ObjectLoader>> result = new ArrayList<>();
        final TreeWalk treeWalk = new TreeWalk(repository);
        final int treeId = treeWalk.addTree(revTree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            final ObjectId objectId = treeWalk.getObjectId(treeId);
            result.add(Tuples.of(treeWalk.getPathString(), reader.open(objectId)));
        }
        return result;
    }

    private String getFileContent(final ObjectLoader loader) {
        if (loader.isLarge()) {
            return cutFileContent(loader, 1, Integer.MAX_VALUE);
        }
        return new String(loader.getBytes(), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    private String cutFileContent(
            final ObjectLoader loader,
            final int from,
            final int to) {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        try (InputStream is = loader.openStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            int skip = from - 1;
            int take = to - skip;
            String line = br.readLine();
            while (line != null) {
                if (skip > 0) {
                    skip--;
                    line = br.readLine();
                    continue;
                }
                joiner.add(line);
                take--;
                if (take == 0) {
                    break;
                }
                line = br.readLine();
            }
        }
        return joiner.toString();
    }
}
