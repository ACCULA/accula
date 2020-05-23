package org.accula.code;

import lombok.RequiredArgsConstructor;
import lombok.Value;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
public class CodeController {
    private static final Logger log = LoggerFactory.getLogger(CodeController.class);
    private static final String BASE_PATH = "code_data/";
    private static final String GITHUB_BASE_URL = "https://github.com/";
    private static final ResponseEntity<byte[]> NOT_FOUND = ResponseEntity.badRequest().build();

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

    private final Map<RepoRef, Repository> cache = new ConcurrentHashMap<>();

    @GetMapping(value = "/{owner}/{repo}/{sha}/**", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> getFile(
            @PathVariable final String owner,
            @PathVariable final String repo,
            @PathVariable final String sha,
            @RequestParam(required = false) final Integer fromLine,
            @RequestParam(required = false) final Integer toLine,
            final HttpServletRequest request) throws GitAPIException, IOException {
        final String file = extractFileName(request);
        final Repository repository = getRepository(owner, repo);
        final Optional<byte[]> bytes = getFileContent(repository, sha, file, fromLine, toLine);
        return bytes.map(ResponseEntity::ok)
                .orElse(NOT_FOUND);
    }

    @NotNull
    private String extractFileName(@NotNull final HttpServletRequest request) {
        return Arrays.stream(request.getRequestURI().split("/"))
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
    private Optional<byte[]> getFileContent(
            @NotNull final Repository repository,
            @NotNull final String sha,
            @NotNull final String file,
            @Nullable Integer fromLine,
            @Nullable Integer toLine) throws IOException {
        final ObjectReader reader = repository.newObjectReader();
        final RevWalk revWalk = new RevWalk(reader);
        final ObjectId commitId = repository.resolve(sha);
        final RevCommit commit = revWalk.parseCommit(commitId);
        final RevTree tree = commit.getTree();
        final TreeWalk treeWalk = TreeWalk.forPath(reader, file, tree);
        if (treeWalk == null) {
            log.error("Cannot find file {} in repo {}, sha={}", file, repository.getDirectory(), sha);
            return Optional.empty();
        }

        final ObjectLoader loader = reader.open(treeWalk.getObjectId(0));
        if (fromLine == null && toLine == null) {
            return Optional.of(loader.getBytes());
        }
        return cutFileContent(loader.openStream(), fromLine, toLine);
    }

    @NotNull
    private Optional<byte[]> cutFileContent(
            @NotNull final InputStream input,
            @Nullable Integer fromLine,
            @Nullable Integer toLine) throws IOException {
        final int from = fromLine != null ? fromLine : 1;
        final int to = toLine != null ? toLine : Integer.MAX_VALUE;
        if (to < from) {
            log.error("Wrong line range: {} - {}", fromLine, toLine);
            return Optional.empty();
        }

        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        ;
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
        return Optional.of(joiner.toString().getBytes());
    }
}
