package org.accula.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * @author Vadim Dyachkov
 */
@Log4j2
@Component
public class CodeHandler {
    private static final String REPOS_BASE_PATH = "code_data/";
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Map<RepoRef, Repository> cache = new ConcurrentHashMap<>();

    @Value
    @RequiredArgsConstructor
    private static class RepoRef {
        String owner;
        String repo;

        public String getUrl() {
            return GITHUB_BASE_URL + toString();
        }

        @Override
        public String toString() {
            return owner + "/" + repo;
        }
    }

    @SneakyThrows
    public Mono<ServerResponse> getFile(final ServerRequest request) {
        final String owner = request.pathVariable("owner");
        final String repo = request.pathVariable("repo");
        final String sha = request.pathVariable("sha");
        final String file = extractFileName(request.uri().getRawPath());
        final int fromLine = request.queryParam("fromLine")
                .map(Integer::parseInt)
                .orElse(1);
        final int toLine = request.queryParam("toLine")
                .map(Integer::parseInt)
                .orElse(Integer.MAX_VALUE);
        return getRepository(owner, repo)
                .flatMap(rep -> getFileContent(rep, sha, file, fromLine, toLine))
                .flatMap(bytes -> ok().body(BodyInserters.fromValue(bytes)))
                .switchIfEmpty(badRequest().build());
    }

    private String extractFileName(final String request) {
        return Arrays.stream(request.split("/"))
                .skip(4)
                .collect(Collectors.joining("/"));
    }

    private Mono<Repository> getRepository(
            final String owner,
            final String repo) {
        final RepoRef ref = new RepoRef(owner, repo);
        final File directory = getDirectory(ref);
        return Mono
                .justOrEmpty(cache.get(ref))
                .switchIfEmpty(openRepository(directory))
                .switchIfEmpty(cloneRepository(ref, directory))
                .map(rep -> {
                    cache.put(ref, rep);
                    return rep;
                });
    }

    private Mono<Repository> cloneRepository(
            final RepoRef ref,
            final File directory) {
        return Mono
                .fromCallable(() -> Git
                        .cloneRepository()
                        .setDirectory(directory)
                        .setURI(ref.getUrl())
                        .call()
                        .getRepository());
    }

    private Mono<Repository> openRepository(final File directory) {
        return Mono
                .just(directory)
                .filter(File::exists)
                .map(this::getFileRepository);
    }

    @SneakyThrows
    private Repository getFileRepository(final File dir) {
        return new FileRepository(new File(dir, ".git"));
    }

    private File getDirectory(final RepoRef ref) {
        return new File(new File(REPOS_BASE_PATH, ref.owner), ref.repo);
    }

    @SneakyThrows
    private Mono<byte[]> getFileContent(
            final Repository repository,
            final String sha,
            final String file,
            final int fromLine,
            final int toLine) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        return Mono
                .fromCallable(() -> getFileInputStream(repository, sha, file, reader, revWalk))
                .map(stream -> cutFileContent(stream, fromLine, toLine))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @SneakyThrows
    private InputStream getFileInputStream(
            final Repository repository,
            final String sha,
            final String file,
            final ObjectReader reader,
            final RevWalk revWalk) {
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree tree = commit.getTree();
        final TreeWalk treeWalk = TreeWalk.forPath(reader, file, tree);
        return reader.open(treeWalk.getObjectId(0)).openStream();
    }

    @SneakyThrows
    private byte[] cutFileContent(
            final InputStream input,
            final int from,
            final int to) {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        try (InputStreamReader is = new InputStreamReader(input);
             BufferedReader br = new BufferedReader(is)) {
            int skip = from - 1;
            int take = to - skip;
            String line = br.readLine();
            while (line != null) {
                if (skip > 0) {
                    skip--;
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
        return joiner.toString().getBytes();
    }
}
