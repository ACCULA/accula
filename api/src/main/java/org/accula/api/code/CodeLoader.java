package org.accula.api.code;

import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Snapshot;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CodeLoader {
    /**
     * Loads all file entities by the commit snapshot
     */
    default Flux<FileEntity<Snapshot>> loadFiles(Snapshot snapshot) {
        return loadFiles(snapshot, FileFilter.ALL);
    }

    /**
     * Loads all file entities that satisfies the filter by the commit snapshot
     */
    Flux<FileEntity<Snapshot>> loadFiles(Snapshot snapshot, FileFilter filter);

    /**
     * Loads the file snippets (file entities with content of the specified line range)
     * by the commit snapshot, the file names and the line ranges
     */
    Flux<FileEntity<Snapshot>> loadSnippets(Snapshot snapshot, List<SnippetMarker> markers);

    /**
     * Loads diff between two commits as {@link DiffEntry} of file entities,
     * each representing two corresponding files in {@code base} and {@code head} commit snapshots.
     * If a file was added in {@code head}, then {@link FileEntity#name()} and {@link FileEntity#content()}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    default Flux<DiffEntry<Snapshot>> loadDiff(Snapshot base, Snapshot head) {
        return loadDiff(base, head, 0, FileFilter.ALL);
    }

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
}
