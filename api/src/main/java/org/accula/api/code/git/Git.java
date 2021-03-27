package org.accula.api.code.git;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.accula.api.util.Iterators;
import org.accula.api.util.Sync;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor
public final class Git {
    private static final String ADDITION = "A";
    private static final String DELETION = "D";
    private static final String MODIFICATION = "M";
    private static final String RENAMING = "R";
    private static final int SUCCESS = 0;
    private static final byte[] NEWLINE = System.lineSeparator().getBytes(UTF_8);
    private static final String ALREADY_EXISTS = "already exists";
    private static final String JOINER_NEWLINE = "";
    private static final long INTERVAL_SINCE_LAST_FETCH_THRESHOLD = Duration.ofSeconds(5L).toMillis();

    private final Map<Path, Repo> repos = new ConcurrentHashMap<>();
    private final Path root;
    private final ExecutorService executor;

    public CompletableFuture<Repo> repo(final Path directory) {
        return readingAsync(directory, () -> {
            final var completePath = root.resolve(directory);
            if (Files.exists(completePath) && Files.isDirectory(completePath)) {
                return repoOf(directory);
            }
            return null;
        });
    }

    public CompletableFuture<Repo> clone(final String url, final String subdirectory) {
        return writingAsync(subdirectory, () -> {
            if (Files.exists(root.resolve(subdirectory))) {
                return repoOf(Path.of(subdirectory));
            }
            try {
                final var process = new ProcessBuilder()
                        .directory(root.toFile())
                        .command("git", "clone", url, subdirectory)
                        .start();
                //TODO: clone timeout
                return process.waitFor() == SUCCESS ? repoOf(Path.of(subdirectory)) : null;
            } catch (IOException | InterruptedException e) {
                throw wrap(e);
            }
        });
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public final class Repo {
        private final Path directory;
        private final Sync sync = new Sync();
        private long lastFetchTs = 0L;

        public CompletableFuture<Repo> fetch() {
            return writingAsync(() -> {
                if (System.currentTimeMillis() - lastFetchTs < INTERVAL_SINCE_LAST_FETCH_THRESHOLD) {
                    return this;
                }
                final var process = git("fetch");
                try {
                    //TODO: fetch timeout
                    final var ret = process.waitFor();
                    lastFetchTs = System.currentTimeMillis();
                    return ret == SUCCESS ? this : null;
                } catch (InterruptedException e) {
                    throw wrap(e);
                }
            });
        }

        public CompletableFuture<List<GitDiffEntry>> diff(final String baseRef,
                                                          final String headRef,
                                                          final int findRenamesMinSimilarityIndex) {
            return readingAsync(() -> {
                final var command = findRenamesMinSimilarityIndex == 0 || findRenamesMinSimilarityIndex == 100
                        ? new String[]{"diff", "--raw", baseRef, headRef}
                        : new String[]{"diff", String.format("-M%02d", findRenamesMinSimilarityIndex), "--raw", baseRef, headRef};

                final var process = git(command);

                return usingStdoutLines(process, lines -> lines
                        .map(Git::parseDiffEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                        .orElse(List.of());
            });
        }

        public <I extends Identifiable> CompletableFuture<Map<I, String>> catFiles(final Iterable<I> identifiableObjects) {
            if (org.accula.api.util.Iterables.isEmptyCollection(identifiableObjects)) {
                return CompletableFuture.completedFuture(Map.of());
            }
            return readingAsync(() -> {
                final var process = git("cat-file", "--batch");

                return usingStdoutLines(process, lines -> {
                    try (var stdin = process.getOutputStream()) {
                        for (final var objectId : identifiableObjects) {
                            stdin.write(objectId.id().getBytes(UTF_8));
                            stdin.write(NEWLINE);
                        }
                        stdin.flush();
                    } catch (IOException e) {
                        throw wrap(e);
                    }
                    return filesContent(lines.iterator(), identifiableObjects);
                }).orElse(Map.of());
            });
        }

        public CompletableFuture<List<GitFile>> show(final String commitSha) {
            return readingAsync(() -> {
                final var process = git("show", "--raw", "--format=oneline", commitSha);

                return usingStdoutLines(process, lines -> lines
                        .map(Git::parseShowEntry)
                        .filter(file -> file != null && !file.isDeleted())
                        .collect(Collectors.toList()))
                        .orElse(List.of());
            });
        }

        public CompletableFuture<Map<GitFile, String>> show(final Iterable<String> commitsSha) {
            return readingAsync(() -> {
                final var show = new ArrayList<String>(4 + (commitsSha instanceof Collection<?> col ? col.size() : 10));
                show.add("show");
                show.add("--raw");
                show.add("--format=oneline");
                Iterables.addAll(show, commitsSha);
                return usingStdoutLines(git(show), lines -> showEntries(lines.iterator()))
                        .orElse(Map.of());
            });
        }

        public CompletableFuture<List<GitFileChanges>> fileChanges(final String commitSha) {
            return readingAsync(() ->
                    usingStdoutLines(git("show", "--format=oneline", commitSha), lines -> changes(lines.iterator()))
                            .orElse(List.of()));
        }

        public CompletableFuture<Map<GitFileChanges, String>> fileChanges(final Iterable<String> commitsSha) {
            return readingAsync(() -> {
                final var show = new ArrayList<String>(3 + (commitsSha instanceof Collection<?> col ? col.size() : 10));
                show.add("show");
                show.add("--format=oneline");
                Iterables.addAll(show, commitsSha);
                return usingStdoutLines(git(show), lines -> changesMap(lines.iterator()))
                        .orElse(Map.of());
            });
        }

        public CompletableFuture<List<GitFile>> lsTree(final String commitSha) {
            return readingAsync(() -> {
                final var process = git("ls-tree", "-r", commitSha);

                return usingStdoutLines(process, lines -> lines
                        .map(Git::parseLsEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                        .orElse(List.of());
            });
        }

        public CompletableFuture<Set<String>> remote() {
            return readingAsync(() -> {
                final var process = git("remote");
                return usingStdoutLines(process, lines -> lines.collect(Collectors.toSet()))
                        .orElse(Set.of());
            });
        }

        public CompletableFuture<Repo> remoteAdd(final String url, final String uniqueName) {
            return writingAsync(() -> {
                final var process = git("remote", "add", "-f", uniqueName, url);
                try {
                    //TODO: remote-add -f timeout
                    final var ret = process.waitFor();
                    final Predicate<Process> remoteAlreadyExists = proc -> usingStderrLines(proc, Stream::findFirst)
                            .orElse("")
                            .contains(ALREADY_EXISTS);
                    return ret == SUCCESS
                            ? this
                            : remoteAlreadyExists.test(process) ? this : null;
                } catch (InterruptedException e) {
                    throw wrap(e);
                }
            });
        }

        public CompletableFuture<Repo> remoteUpdate(final String name) {
            return writingAsync(() -> {
                final var process = git("remote", "update", name);
                try {
                    //TODO: remote update timeout
                    return process.waitFor() == SUCCESS ? this : null;
                } catch (InterruptedException e) {
                    throw wrap(e);
                }
            });
        }

        public CompletableFuture<List<GitCommit>> log(final String ref) {
            return readingAsync(() ->
                    usingStdoutLines(git("log", "--date=rfc", ref), lines ->
                            commits(lines.iterator()))
                            .orElse(List.of()));
        }

        public CompletableFuture<List<GitCommit>> log(final String fromRefExclusive, final String toRefInclusive) {
            return readingAsync(() -> {
                final var log = git("log", "--date=rfc", "%s..%s".formatted(fromRefExclusive, toRefInclusive));
                return usingStdoutLines(log, lines ->
                        commits(lines.iterator()))
                        .orElse(List.of());
            });
        }

        public CompletableFuture<List<GitCommit>> revListAllPretty() {
            return readingAsync(() -> {
                final var log = git("rev-list", "--pretty", "--all", "--date=rfc");
                return usingStdoutLines(log, lines ->
                        commits(lines.iterator()))
                        .orElse(List.of());
            });
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

        private Process git(final ArrayList<String> command) {
            try {
                command.add(0, "git");
                return new ProcessBuilder(command)
                        .directory(root.resolve(directory).toFile())
                        .start();
            } catch (IOException e) {
                throw wrap(e);
            }
        }

        private <T> CompletableFuture<T> readingAsync(final Supplier<T> readOp) {
            return CompletableFuture.supplyAsync(sync.reading(readOp), executor);
        }

        private <T> CompletableFuture<T> writingAsync(final Supplier<T> writeOp) {
            return CompletableFuture.supplyAsync(sync.writing(writeOp), executor);
        }
    }

    private static <I extends Identifiable> Map<I, String> filesContent(final Iterator<String> lines,
                                                                        final Iterable<I> identifiableObjects) {
        final Iterator<I> identifiableObjectsIterator = identifiableObjects.iterator();
        if (!lines.hasNext() || !identifiableObjectsIterator.hasNext()) {
            return Map.of();
        }
        final Map<I, String> filesContent = new HashMap<>((identifiableObjects instanceof Collection<?> col) ? col.size() : 16);
        I prevObjIdentifiable = null;
        I currObjIdentifiable = identifiableObjectsIterator.next();
        int currentFileLineCounter = 1;
        StringJoiner currentFile = null;
        int fromLine = Integer.MIN_VALUE;
        int toLine = Integer.MAX_VALUE;
        while (lines.hasNext()) {
            final var line = lines.next();
            if ((identifiableObjectsIterator.hasNext() || currObjIdentifiable != null) && line.startsWith(currObjIdentifiable.id())) {
                currentFileLineCounter = 1;
                if (currObjIdentifiable instanceof Snippet snippet) {
                    final var range = snippet.lines();
                    fromLine = range.from();
                    toLine = range.to();
                } else {
                    fromLine = Integer.MIN_VALUE;
                    toLine = Integer.MAX_VALUE;
                }
                if (prevObjIdentifiable != null && currentFile.length() > 0) {
                    filesContent.put(prevObjIdentifiable, currentFile.toString());
                }
                prevObjIdentifiable = currObjIdentifiable;
                if (identifiableObjectsIterator.hasNext()) {
                    currObjIdentifiable = identifiableObjectsIterator.next();
                } else {
                    currObjIdentifiable = null;
                }

                currentFile = new StringJoiner(System.lineSeparator());
                continue;
            }
            final var lineNumber = currentFileLineCounter++;
            if (lineNumber >= fromLine && lineNumber <= toLine) {
                currentFile.add(line);
            }
            if (lineNumber == toLine) {
                currentFile.add(JOINER_NEWLINE);
            }
        }
        if (prevObjIdentifiable != null && currentFile.length() > 0) {
            filesContent.put(prevObjIdentifiable, currentFile.toString());
        }
        return filesContent;
    }

    /**
     * Forms a {@link GitCommit} list by extracting the components from the given lines using {@link CommitEntryParseIterator}
     *
     * @implNote We expect that commit date is in RFC-1123 format, so we
     * can easily convert it to {@link Instant} using built-in {@link DateTimeFormatter}
     * @see Git.Repo#log(String, String)
     * @see Repo#revListAllPretty()
     */
    private static List<GitCommit> commits(final Iterator<String> lines) {
        final List<GitCommit> commits = new ArrayList<>();
        final var cb = new Object() {
            GitCommit.GitCommitBuilder commitBuilder;
        };
        new CommitEntryParseIterator(lines).forEachRemaining(entry -> {
            final var line = entry.line();
            switch (entry.type()) {
                case SHA -> {
                    cb.commitBuilder = GitCommit.builder();
                    cb.commitBuilder.sha(line);
                }
                case MERGE -> cb.commitBuilder.isMerge(true);
                case AUTHOR -> {
                    final var nameAndEmail = line.split(" <", 2);
                    if (nameAndEmail.length != 2) {
                        throw new IllegalStateException("'Author:' line was of not supported format: %s" .formatted(line));
                    }
                    cb.commitBuilder.authorName(nameAndEmail[0]);
                    cb.commitBuilder.authorEmail(nameAndEmail[1].substring(0, nameAndEmail[1].length() - 1));
                }
                case DATE -> {
                    cb.commitBuilder.date(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(line)));
                    commits.add(cb.commitBuilder.build());
                }
            }
        });
        return commits;
    }

    private static Map<GitFile, String> showEntries(final Iterator<String> lines) {
        if (!lines.hasNext()) {
            return Map.of();
        }
        final var entries = new LinkedHashMap<GitFile, String>();
        var sha = (String) null;
        while (lines.hasNext()) {
            final var line = lines.next();
            if (line.isEmpty()) {
                continue;
            }
            final var entry = parseShowEntry(line);
            if (entry == null) {
                if (!lines.hasNext()) {
                    break;
                }
                sha = line.substring(0, 40);
            } else if (!entry.isDeleted()) {
                entries.put(entry, sha);
            }
        }
        return entries;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static List<GitFileChanges> changes(final Iterator<String> lines) {
        if (!lines.hasNext()) {
            return List.of();
        }
        return Streams.stream(new FileChangesParseIterator(Iterators.nextResettable(lines)))
                .filter(GitFileChanges.class::isInstance)
                .map(GitFileChanges.class::cast)
                .collect(Collectors.toList());
    }

    private static Map<GitFileChanges, String> changesMap(final Iterator<String> lines) {
        if (!lines.hasNext()) {
            return Map.of();
        }
        final var entries = new LinkedHashMap<GitFileChanges, String>();
        final var nextResettableIter = Iterators.nextResettable(lines);
        final var iter = new FileChangesParseIterator(nextResettableIter);
        var sha = (String) null;
        while (iter.hasNext()) {
            final var next = iter.next();
            if (next instanceof String sh) {
                sha = sh.substring(0, 40);
                continue;
            }
            final var fileChanges = (GitFileChanges) next;
            if (!fileChanges.file().isDeleted() && !fileChanges.changedLines().isEmpty()) {
                entries.put(fileChanges, Objects.requireNonNull(sha, "sha MUST not be null here"));
            }
        }
        return entries;
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
    private static GitFile parseShowEntry(final String line) {
        if (line.isEmpty() || line.charAt(0) != ':') {
            return null;
        }
        final var components = line.split("\\s+");
        if (components.length < 6 || components.length > 9) {
            return null;
        }
        return switch (components.length) {
            case 6 -> GitFile.of(components[3], components[5]);
            case 7 -> GitFile.of(components[3], components[6]);
            case 8 -> GitFile.of(components[5], components[7]);
            case 9 -> GitFile.of(components[5], components[8]);
            default -> null;
        };
    }

    /// Line format:
    ///      0        1         2         3
    /// file_mode file_type object_id filename
    @Nullable
    private static GitFile parseLsEntry(final String line) {
        final var components = line.split("\\s+");
        if (components.length != 4) {
            return null;
        }
        return GitFile.of(components[2], components[3]);
    }

    private static <T> Optional<T> usingStdoutLines(final Process process, final Function<Stream<String>, T> stdoutLinesUse) {
        try (var stdoutLines = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8)).lines()) {
            final var res = stdoutLinesUse.apply(stdoutLines);
            try {
                return process.waitFor() == SUCCESS ? Optional.of(res) : Optional.empty();
            } catch (InterruptedException e) {
                throw wrap(e);
            }
        }
    }

    private static <T> T usingStderrLines(final Process process, final Function<Stream<String>, T> stderrLinesUse) {
        try (var stdoutLines = new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8)).lines()) {
            try {
                process.waitFor();
                return stderrLinesUse.apply(stdoutLines);
            } catch (InterruptedException e) {
                throw wrap(e);
            }
        }
    }

    private <T> CompletableFuture<T> readingAsync(final Path directory, final Supplier<T> readOp) {
        return CompletableFuture.supplyAsync(safe(directory).reading(readOp), executor);
    }

    private <T> CompletableFuture<T> writingAsync(final String directory, final Supplier<T> writeOp) {
        return CompletableFuture.supplyAsync(safe(directory).writing(writeOp), executor);
    }

    private Sync safe(final String key) {
        return safe(Path.of(key));
    }

    private Sync safe(final Path path) {
        return repoOf(path).sync;
    }

    private Repo repoOf(final Path path) {
        return repos.computeIfAbsent(path, Repo::new);
    }

    private static GitException wrap(final Throwable e) {
        return new GitException(e);
    }
}
