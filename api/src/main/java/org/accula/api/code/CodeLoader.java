package org.accula.api.code;

import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
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
    default Flux<FileEntity> loadFiles(CommitSnapshot snapshot) {
        return loadFiles(snapshot, FileFilter.ALL);
    }

    /**
     * Loads all file entities that satisfies the filter by the commit snapshot
     */
    Flux<FileEntity> loadFiles(CommitSnapshot snapshot, FileFilter filter);

    /**
     * Loads the file snippets (file entities with content of the specified line range)
     * by the commit snapshot, the file names and the line ranges
     */
    Flux<FileEntity> loadSnippets(CommitSnapshot snapshot, List<SnippetMarker> markers);

    /**
     * Loads diff between two commits as {@link DiffEntry} of file entities,
     * each representing two corresponding files in {@code base} and {@code head} commit snapshots.
     * If a file was added in {@code head}, then {@link FileEntity#getName} and {@link FileEntity#getContent}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    default Flux<DiffEntry> loadDiff(CommitSnapshot base, CommitSnapshot head) {
        return loadDiff(base, head, FileFilter.ALL);
    }

    /**
     * Loads diff between two commits as {@link DiffEntry} of file entities that satisfy the filter,
     * each representing two corresponding files in {@code base} and {@code head} commit snapshots.
     * If a file was added in {@code head}, then {@link FileEntity#getName} and {@link FileEntity#getContent}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    Flux<DiffEntry> loadDiff(CommitSnapshot base, CommitSnapshot head, FileFilter filter);

    /**
     * Loads diff between two commits of remote repositories.
     *
     * @see #loadDiff(CommitSnapshot, CommitSnapshot, FileFilter)
     */
    Flux<DiffEntry> loadRemoteDiff(GithubRepo projectRepo, CommitSnapshot base, CommitSnapshot head, FileFilter filter);
}
