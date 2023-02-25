package org.accula.api.code;

import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Snapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CodeLoader {
    /**
     * Loads all file entities that satisfies the filter and have been modified during specified commits
     */
    Flux<FileEntity<Snapshot>> loadFiles(GithubRepo repo, Iterable<Snapshot> snapshots, FileFilter filter);

    /**
     * Loads the file snippets (file entities with content of the specified line range)
     * by the commit snapshot, the file names and the line ranges
     */
    Flux<FileEntity<Snapshot>> loadSnippets(Snapshot snapshot, List<SnippetMarker> markers);

    /**
     * Loads diff between two commits as {@link DiffEntry} of file entities that satisfy the filter,
     * each representing two corresponding files in {@code base} and {@code head} commit snapshots.
     * If a file was added in {@code head}, then {@link FileEntity#name()} and {@link FileEntity#content()}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    Flux<DiffEntry<Snapshot>> loadDiff(Snapshot base, Snapshot head, int minSimilarityIndex, FileFilter filter);

    /**
     * Loads diff between two commits of remote repositories.
     *
     * @see #loadDiff(Snapshot, Snapshot, int, FileFilter)
     */
    Flux<DiffEntry<Snapshot>> loadRemoteDiff(GithubRepo repo, Snapshot base, Snapshot head, int minSimilarityIndex, FileFilter filter);

    /**
     * Loads filenames by the project repo.
     */
    Flux<String> loadFilenames(GithubRepo projectRepo);

    /**
     * Loads all the commits
     */
    Flux<Commit> loadAllCommits(GithubRepo repo);

    /**
     * Loads commits of specified repo in a given ref interval (sinceRefExclusive, untilRefInclusive]
     */
    Flux<Commit> loadCommits(GithubRepo repo, String sinceRefExclusive, String untilRefInclusive);

    /**
     * Loads commit by specified ref for the repo
     */
    Mono<Commit> loadCommit(GithubRepo repo, String ref);

    /**
     * Loads commits of specified repo from the star to the given ref.
     */
    Flux<Commit> loadCommits(GithubRepo repo, String ref);

    /**
     * Loads all the commits sha
     */
    Mono<Set<String>> loadAllCommitsSha(GithubRepo repo);
}
