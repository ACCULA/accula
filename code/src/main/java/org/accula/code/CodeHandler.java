package org.accula.code;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Log4j2
@Component
public class CodeHandler {
    private static final String BASE_PATH = "code_data/";
    private static final String GITHUB_BASE_URL = "https://github.com/";

    private final Map<RepoRef, Repository> cache = new ConcurrentHashMap<>();

    @Value
    @RequiredArgsConstructor
    private static class RepoRef {
        String owner;
        String repo;

        @NotNull
        public String getUrl() {
            return GITHUB_BASE_URL + toString();
        }

        @Override
        public String toString() {
            return owner + "/" + repo;
        }
    }

    @NotNull
    @SneakyThrows
    public Mono<ServerResponse> getFile(final ServerRequest request) {
        final String owner = request.pathVariable("owner");
        final String repo = request.pathVariable("repo");
        final String sha = request.pathVariable("sha");
        final String file = extractFileName(request.uri().getRawPath());
        final Optional<Integer> fromLine = request.queryParam("fromLine")
                .map(Integer::parseInt);
        final Optional<Integer> toLine = request.queryParam("toLine")
                .map(Integer::parseInt);
        return Mono.fromCallable(() -> getRepository(owner, repo))
                .flatMap(rep -> getFileContent(rep, sha, file, fromLine, toLine))
                .flatMap(bytes -> ok().body(BodyInserters.fromValue(bytes)))
                .switchIfEmpty(badRequest().build());
    }

    @NotNull
    private String extractFileName(@NotNull final String request) {
        return Arrays.stream(request.split("/"))
                .skip(4)
                .collect(Collectors.joining("/"));
    }

    @NotNull
    private Repository getRepository(
            @NotNull final String owner,
            @NotNull final String repo) throws GitAPIException, IOException {
        final RepoRef ref = new RepoRef(owner, repo);
        final File directory = getDirectory(ref);
        final Repository repository;
        if (!directory.exists()) {
            log.debug("Creating directory {}", directory);
            repository = Git.cloneRepository()
                    .setDirectory(directory)
                    .setURI(ref.getUrl())
                    .call()
                    .getRepository();
            cache.put(ref, repository);
        } else if (!cache.containsKey(ref)) {
            log.debug("Cache miss for {}", ref);
            repository = new FileRepository(new File(directory, ".git"));
            Git.wrap(repository).pull().call();
            cache.put(ref, repository);
        } else {
            log.debug("Cache hit for {}", ref);
            repository = cache.get(ref);
        }
        return repository;
    }

    @NotNull
    private File getDirectory(@NotNull final RepoRef ref) {
        return new File(new File(BASE_PATH, ref.owner), ref.repo);
    }

    @NotNull
    @SneakyThrows
    private Mono<byte[]> getFileContent(
            @NotNull final Repository repository,
            @NotNull final String sha,
            @NotNull final String file,
            @NotNull final Optional<Integer> fromLine,
            @NotNull final Optional<Integer> toLine) {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree tree = commit.getTree();
        final TreeWalk treeWalk = TreeWalk.forPath(reader, file, tree);
        if (treeWalk == null) {
            log.error("Cannot find file {} in repo {}, sha={}", file, repository.getDirectory(), sha);
            return Mono.empty();
        }

        final ObjectLoader loader = reader.open(treeWalk.getObjectId(0));
        if (fromLine.isEmpty() && toLine.isEmpty()) {
            return Mono.just(loader.getBytes());
        }
        final int from = fromLine.orElse(1);
        final int to = toLine.orElse(Integer.MAX_VALUE);
        if (to < from) {
            log.error("Wrong line range: {} - {}", fromLine, toLine);
            return Mono.empty();
        }
        return cutFileContent(loader.openStream(), from, to);
    }

    @NotNull
    private Mono<byte[]> cutFileContent(
            @NotNull final InputStream input,
            final int from,
            final int to) throws IOException {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        try (InputStreamReader in = new InputStreamReader(input);
             BufferedReader br = new BufferedReader(in)) {
            int skip = from - 1;
            int take = to - skip;
            String line;
            while ((line = br.readLine()) != null) {
                if (skip > 0) {
                    skip--;
                    continue;
                }
                joiner.add(line);
                take--;
                if (take == 0) {
                    break;
                }
            }
        }
        return Mono.just(joiner.toString().getBytes());
    }
}
