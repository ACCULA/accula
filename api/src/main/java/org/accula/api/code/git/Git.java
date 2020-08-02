package org.accula.api.code.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.accula.api.util.Lambda;
import org.accula.api.util.Sync;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

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
    private static final byte[] NEWLINE = System.lineSeparator().getBytes(UTF_8);
    private static final String ALREADY_EXISTS = "already exists";

    private final Map<String, Sync> syncs = new ConcurrentHashMap<>();
    private final Path root;
    private final ExecutorService executor;

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
                        return new Repo(Path.of(subdirectory));
                    }
                    try {
                        final var process = new ProcessBuilder()
                                .directory(root.toFile())
                                .command("git", "clone", url, subdirectory)
                                .start();
                        //TODO: clone timeout
                        return process.waitFor() == SUCCESS ? new Repo(Path.of(subdirectory)) : null;
                    } catch (IOException | InterruptedException e) {
                        throw wrap(e);
                    }
                }), executor);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public final class Repo {
        private final Path directory;

        public CompletableFuture<Repo> fetch() {
            return CompletableFuture.supplyAsync(writing(() -> {
                final var process = git("fetch");
                try {
                    //TODO: fetch timeout
                    final var ret = process.waitFor();
                    return ret == SUCCESS ? this : null;
                } catch (InterruptedException e) {
                    throw wrap(e);
                }
            }), executor);
        }

        public CompletableFuture<List<DiffEntry>> diff(final String baseRef,
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

        public CompletableFuture<Map<? super Identifiable, String>> catFiles(final List<? extends Identifiable> objectIds) {
            return CompletableFuture
                    .supplyAsync(reading(() -> {
                        final var process = git("cat-file", "--batch");

                        return usingStdoutLines(process, Collections.emptyMap(), lines -> {
                            try (final var stdin = process.getOutputStream()) {
                                for (final var objectId : objectIds) {
                                    stdin.write(objectId.getId().getBytes(UTF_8));
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

        @SuppressWarnings("unused")
        /// Once we'll start to detecting clones in commits, not revisions, the method will be in use
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
                        return usingStdoutLines(process, Collections.emptySet(), lines -> lines.collect(toSet()));
                    }), executor);
        }

        public CompletableFuture<Repo> remoteAdd(final String url, final String uniqueName) {
            return CompletableFuture
                    .supplyAsync(writing(() -> {
                        final var process = git("remote", "add", "-f", uniqueName, url);
                        try {
                            //TODO: remote-add -f timeout
                            final var ret = process.waitFor();
                            final Predicate<Process> remoteAlreadyExists = proc -> usingStderrLines(proc, Stream::findFirst)
                                    .orElse("")
                                    .contains(ALREADY_EXISTS);
                            return ret == SUCCESS ? this : remoteAlreadyExists.test(process) ? this : null;
                        } catch (InterruptedException e) {
                            throw wrap(e);
                        }
                    }), executor);
        }

        public CompletableFuture<Repo> remoteUpdate(final String name) {
            return CompletableFuture
                    .supplyAsync(writing(() -> {
                        final var process = git("remote", "update", name);
                        try {
                            //TODO: remote update timeout
                            return process.waitFor() == SUCCESS ? this : null;
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

    private static Map<? super Identifiable, String> filesContent(final List<String> lines, final List<? extends Identifiable> objectIds) {
        if (objectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<? super Identifiable, String> filesContent = new HashMap<>(objectIds.size());
        int fileToDiscoverIdx = 0;
        int currentFileLineCounter = 1;
        StringJoiner currentFile = null;
        int fromLine = Integer.MIN_VALUE;
        int toLine = Integer.MAX_VALUE;
        for (final var line : lines) {
            final Identifiable identifiable;
            if (fileToDiscoverIdx < objectIds.size() && line.startsWith((identifiable = objectIds.get(fileToDiscoverIdx)).getId())) {
                currentFileLineCounter = 1;
                if (identifiable instanceof Snippet) {
                    final var snippet = (Snippet) identifiable;
                    fromLine = snippet.getFromLine();
                    toLine = snippet.getToLine();
                } else {
                    fromLine = Integer.MIN_VALUE;
                    toLine = Integer.MAX_VALUE;
                }
                if (fileToDiscoverIdx != 0 && currentFile.length() > 0) {
                    filesContent.put(objectIds.get(fileToDiscoverIdx - 1), currentFile.toString());
                }
                ++fileToDiscoverIdx;

                currentFile = new StringJoiner(System.lineSeparator());
                continue;
            }
            final var lineNumber = currentFileLineCounter++;
            if (lineNumber >= fromLine && lineNumber <= toLine) {
                currentFile.add(line);
            }
            if (lineNumber == toLine) {
                currentFile.add("");
            }
        }
        if (fileToDiscoverIdx != 0 && currentFile.length() > 0) {
            filesContent.put(objectIds.get(fileToDiscoverIdx - 1), currentFile.toString());
        }
        return filesContent;
    }

    /// Line format:
    ///      0                1                2              3        4        5             6
    /// :base_file_mode head_file_mode base_object_id head_object_id type base_filename head_filename
    @Nullable
    private static DiffEntry parseDiffEntry(final String line) {
        final var components = line.split("\\s+");
        switch (components.length) {
            case 6:
                return switch (components[4]) {
                    case ADDITION -> DiffEntry.addition(components[3], components[5]);
                    case DELETION -> DiffEntry.deletion(components[2], components[5]);
                    case MODIFICATION -> DiffEntry.modification(components[2], components[3], components[5]);
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

                return DiffEntry.renaming(components[2], components[5], components[3], components[6], similarityIndex);
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
        if (line.isEmpty() || line.charAt(0) != ':') {
            return null;
        }
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
        try (final var stdoutLines = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8)).lines()) {
            final var res = stdoutLinesUse.apply(stdoutLines);
            try {
                return process.waitFor() == SUCCESS ? res : fallback;
            } catch (InterruptedException e) {
                throw wrap(e);
            }
        }
    }

    private static <T> T usingStderrLines(final Process process, final Function<Stream<String>, T> stderrLinesUse) {
        try (final var stdoutLines = new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8)).lines()) {
            try {
                process.waitFor();
                return stderrLinesUse.apply(stdoutLines);
            } catch (InterruptedException e) {
                throw wrap(e);
            }
        }
    }

    private Sync safe(final String key) {
        return syncs.computeIfAbsent(key, Lambda.expandingWithArg(Sync::new));
    }

    private Sync safe(final Path path) {
        return safe(path.toString());
    }

    private static GitException wrap(final Throwable e) {
        return new GitException(e);
    }
}
