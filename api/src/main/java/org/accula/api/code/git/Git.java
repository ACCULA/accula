package org.accula.api.code.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.accula.api.code.FileEntity;
import org.accula.api.code.git.GitDiffEntry.Addition;
import org.accula.api.code.git.GitDiffEntry.Deletion;
import org.accula.api.code.git.GitDiffEntry.Modification;
import org.accula.api.code.git.GitDiffEntry.Renaming;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.util.Sync;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class Git {
    private static final String ADDITION = "A";
    private static final String DELETION = "D";
    private static final String MODIFICATION = "M";
    private static final String RENAMING = "R";
    private static final String DELETED_OBJECT_ID = "0000000";
    private static final int SUCCESS = 0;
    private static final byte[] NEWLINE = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
    private static final String ALREADY_EXISTS = "already exists";

    private final Map<String, Sync> syncs = new ConcurrentHashMap<>();
    private final Path root;
    private final ExecutorService executor;

    public static void main(String[] args) throws Throwable {
        final var git = new Git(Paths.get("/Users/anton.lamtev/Downloads/"), Executors.newFixedThreadPool(10));
        final var repo = git.repo(Paths.get("2020-db-lsm")).get();

        Mono
                .fromFuture(repo.remoteAdd("https://github.com/zvladn7/2020-db-lsm.git", "zvladn7"))
                .subscribe(v -> {
                    System.out.println(v);
                });

        final var flux = Mono
                .fromFuture(repo.diff("335573f171c8094d451ada860614f6fae968899b", "f3e55115145d02dd40dc73c9bf7b3114bf9bc226", 1))
                .flatMapMany(diffEntries -> Mono
                        .fromFuture(repo
                                .catFiles(diffEntries
                                        .stream()
                                        .flatMap(GitDiffEntry::objectIds)
                                        .collect(Collectors.toList())))
                        .map(files -> diffEntries
                                .stream()
                                .map(diffEntry -> {
                                    CommitSnapshot c = null;
                                    if (diffEntry instanceof Addition) {
                                        final var addition = (Addition) diffEntry;
                                        return org.accula.api.code.DiffEntry.of(FileEntity.absent(c), new FileEntity(c, addition.head.name, files.get(addition.head.objectId)));
                                    } else if (diffEntry instanceof Deletion) {
                                        final var deletion = (Deletion) diffEntry;
                                        return org.accula.api.code.DiffEntry.of(new FileEntity(c, deletion.base.name, files.get(deletion.base.objectId)), FileEntity.absent(c));
                                    } else if (diffEntry instanceof Modification) {
                                        final var modification = (Modification) diffEntry;
                                        return org.accula.api.code.DiffEntry.of(
                                                new FileEntity(c, modification.base.name, files.get(modification.base.objectId)),
                                                new FileEntity(c, modification.head.name, files.get(modification.head.objectId)));
                                    } else if (diffEntry instanceof Renaming) {
                                        final var renaming = (Renaming) diffEntry;
                                        return new org.accula.api.code.DiffEntry(
                                                new FileEntity(c, renaming.base.name, files.get(renaming.base.objectId)),
                                                new FileEntity(c, renaming.head.name, files.get(renaming.head.objectId)),
                                                renaming.similarityIndex);
                                    } else {
                                        return null;
                                    }
                                }))
                        .flatMapMany(Flux::fromStream))
                .subscribe(res -> {
                    System.out.println(res);
                });
    }

    public CompletableFuture<Repo> repo(final Path directory) {
        return CompletableFuture
                .supplyAsync(safe(directory).reading(() -> {
                    final var completePath = root.resolve(directory);
                    if (Files.exists(completePath) && Files.isDirectory(completePath)) {
                        return new Repo(directory);
                    }
                    return null;
                }), executor);
    }

    public CompletableFuture<Repo> clone(final String url, final String subdirectory) {
        return CompletableFuture
                .supplyAsync(safe(subdirectory).writing(() -> {
                    if (Files.exists(root.resolve(subdirectory))) {
                        return new Repo(Paths.get(subdirectory));
                    }
                    try {
                        final var process = new ProcessBuilder()
                                .directory(root.toFile())
                                .command("git", "clone", url, subdirectory)
                                .start();
                        //TODO: clone timeout
                        return process.waitFor() == SUCCESS ? new Repo(Paths.get(subdirectory)) : null;
                    } catch (IOException | InterruptedException e) {
                        throw wrap(e);
                    }
                }), executor);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public final class Repo {
        private final Path directory;

        public CompletableFuture<List<GitDiffEntry>> diff(final String baseRef,
                                                          final String headRef,
                                                          final int findRenamesMinSimilarityIndex) {
            return CompletableFuture
                    .supplyAsync(reading(() -> {
                        final var command = findRenamesMinSimilarityIndex == 0 || findRenamesMinSimilarityIndex == 100
                                ? new String[]{"diff", "--raw", baseRef, headRef}
                                : new String[]{"diff", String.format("-M%02d", findRenamesMinSimilarityIndex), "--raw", baseRef, headRef};

                        final var process = git(command);

                        return usingStdoutLines(process, Collections.emptyList(), lines -> lines
                                .map(Git::parseDiffEntry)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                    }), executor);
        }

        public CompletableFuture<Map<String, String>> catFiles(final List<String> objectIds) {
            return CompletableFuture
                    .supplyAsync(reading(() -> {
                        final var process = git("cat-file", "--batch");

                        return usingStdoutLines(process, Collections.emptyMap(), lines -> {
                            try (final var stdin = process.getOutputStream()) {
                                for (String objectId : objectIds) {
                                    stdin.write(objectId.getBytes(StandardCharsets.UTF_8));
                                    stdin.write(NEWLINE);
                                }
                                stdin.flush();
                            } catch (IOException e) {
                                throw wrap(e);
                            }

                            final var lineList = lines.collect(Collectors.toList());
                            return filesContent(lineList, objectIds);
                        });
                    }), executor);
        }

        public CompletableFuture<List<File>> show(final String commitSha) {
            return CompletableFuture
                    .supplyAsync(reading(() -> {
                        final var process = git("show", "--raw", commitSha);

                        return usingStdoutLines(process, Collections.emptyList(), lines -> lines
                                .map(Git::parseShowEntry)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                    }), executor);
        }

        public CompletableFuture<List<File>> lsTree(final String commitSha) {
            return CompletableFuture
                    .supplyAsync(reading(() -> {
                        final var process = git("ls-tree", "-r", commitSha);

                        return usingStdoutLines(process, Collections.emptyList(), lines -> lines
                                .map(Git::parseLsEntry)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                    }), executor);
        }

        public CompletableFuture<Set<String>> remote() {
            return CompletableFuture
                    .supplyAsync(reading(() -> {
                        final var process = git("remote");
                        return usingStdoutLines(process, Collections.emptySet(), lines -> lines.collect(Collectors.toSet()));
                    }), executor);
        }

        public CompletableFuture<Boolean> remoteAdd(final String url, final String uniqueName) {
            return CompletableFuture
                    .supplyAsync(writing(() -> {
                        final var process = git("remote", "add", "-f", uniqueName, url);
                        try {
                            //TODO: remote-add -f timeout
                            final var ret = process.waitFor();
                            final Predicate<Process> remoteAlreadyExists = proc -> usingStderrLines(proc, Stream::findFirst)
                                    .orElse("")
                                    .contains(ALREADY_EXISTS);
                            return ret == SUCCESS ? Boolean.TRUE : remoteAlreadyExists.test(process) ? Boolean.TRUE : Boolean.FALSE;
                        } catch (InterruptedException e) {
                            throw wrap(e);
                        }
                    }), executor);
        }

        public CompletableFuture<Boolean> remoteUpdate(final String name) {
            return CompletableFuture
                    .supplyAsync(writing(() -> {
                        final var process = git("remote", "update", name);
                        try {
                            //TODO: remote update timeout
                            return process.waitFor() == SUCCESS ? Boolean.TRUE : Boolean.FALSE;
                        } catch (InterruptedException e) {
                            throw wrap(e);
                        }
                    }), executor);
        }

        private Process git(final String... command) {
            try {
                final var cmd = new ArrayList<String>();
                cmd.add("git");
                cmd.addAll(List.of(command));
                return new ProcessBuilder(cmd)
                        .directory(root.resolve(directory).toFile())
                        .start();
            } catch (IOException e) {
                throw wrap(e);
            }
        }

        private <T> Supplier<T> reading(final Supplier<T> readOp) {
            return safe(directory).reading(readOp);
        }

        private <T> Supplier<T> writing(final Supplier<T> writeOp) {
            return safe(directory).writing(writeOp);
        }
    }

    private static Map<String, String> filesContent(final List<String> lines, final List<String> objectIds) {
        int fileIdx = 0;
        final var filesContent = new HashMap<String, String>(objectIds.size());
        StringJoiner currentFile = null;
        for (int i = 0; i < lines.size() && fileIdx < objectIds.size(); ++i) {
            final var line = lines.get(i);
            if (line.startsWith(objectIds.get(fileIdx))) {
                if (fileIdx != 0) {
                    filesContent.put(objectIds.get(fileIdx - 1), currentFile.toString());
                }
                ++fileIdx;
                currentFile = new StringJoiner(System.lineSeparator());
                continue;
            }
            Objects.requireNonNull(currentFile).add(line);
        }
        return filesContent;
    }

    /// Line format:
    ///      0                1                2              3        4        5             6
    /// :base_file_mode head_file_mode base_object_id head_object_id type base_filename head_filename
    @Nullable
    private static GitDiffEntry parseDiffEntry(final String line) {
        final var components = line.split("\\s+");
        switch (components.length) {
            case 6:
                return switch (components[4]) {
                    case ADDITION -> GitDiffEntry.addition(components[3], components[5]);
                    case DELETION -> GitDiffEntry.deletion(components[2], components[5]);
                    case MODIFICATION -> GitDiffEntry.modification(components[2], components[3], components[5]);
                    default -> null;
                };
            case 7: {
                final var typeComponent = components[4];
                if (!typeComponent.startsWith(RENAMING)) {
                    return null;
                }

                int similarityIndex;
                try {
                    similarityIndex = Integer.parseInt(typeComponent.substring(1));
                } catch (NumberFormatException e) {
                    similarityIndex = 0;
                }

                return GitDiffEntry.renaming(components[2], components[5], components[3], components[6], similarityIndex);
            }
            default:
                return null;
        }
    }

    /// Line format:
    ///      0                1                2              3        4        5             6
    /// :base_file_mode head_file_mode base_object_id head_object_id type base_filename head_filename
    @Nullable
    private static File parseShowEntry(final String line) {
        final var components = line.split("\\s+");
        if (components.length != 6 && components.length != 7) {
            return null;
        }
        final var objectId = components[3];
        if (objectId.startsWith(DELETED_OBJECT_ID)) {
            return null;
        }
        return switch (components.length) {
            case 6 -> File.of(objectId, components[5]);
            case 7 -> File.of(objectId, components[6]);
            default -> null;
        };
    }

    /// Line format:
    ///      0        1         2         3
    /// file_mode file_type object_id filename
    @Nullable
    private static File parseLsEntry(final String line) {
        final var components = line.split("\\s+");
        if (components.length != 4) {
            return null;
        }
        return File.of(components[2], components[3]);
    }

    private static <T> T usingStdoutLines(final Process process, final T fallback, final Function<Stream<String>, T> stdoutLinesUse) {
        try (final var stdoutLines = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()) {
            final var res = stdoutLinesUse.apply(stdoutLines);
            try {
                return process.waitFor() == SUCCESS ? res : fallback;
            } catch (InterruptedException e) {
                throw wrap(e);
            }
        }
    }

    private static <T> T usingStderrLines(final Process process, final Function<Stream<String>, T> stderrLinesUse) {
        try (final var stdoutLines = new BufferedReader(new InputStreamReader(process.getErrorStream())).lines()) {
            try {
                process.waitFor();
                return stderrLinesUse.apply(stdoutLines);
            } catch (InterruptedException e) {
                throw wrap(e);
            }
        }
    }

    private Sync safe(final String key) {
        return syncs.computeIfAbsent(key, Sync::create);
    }

    private Sync safe(final Path path) {
        return safe(path.toString());
    }

    private static GitException wrap(final Throwable e) {
        return new GitException(e);
    }
}
