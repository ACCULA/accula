package org.accula.api.code;

import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CodeLoader {
    /**
     * Get all file entities by the commit snapshot
     */
    default Flux<FileEntity> getFiles(CommitSnapshot snapshot) {
        return getFiles(snapshot, FileFilter.ALL);
    }

    /**
     * Get all file entities that satisfies the filter by the commit snapshot
     */
    Flux<FileEntity> getFiles(CommitSnapshot snapshot, FileFilter filter);

    /**
     * Get the file snippet (file entity with content from the specified line range)
     * by the commit snapshot, the file name and the line range
     *
     * @deprecated use {@link CodeLoader#getFileSnippets(List)} instead
     */
    Mono<FileEntity> getFileSnippet(CommitSnapshot snapshot, String filename, int fromLine, int toLine);

    default Flux<FileEntity> getFileSnippets(List<SnippetMarker> markers) {
        return Flux.empty();
    }

    /**
     * Get diff between two commits as {@link DiffEntry} of file entities,
     * each representing two corresponding files in {@code base} and {@code head} commit snapshots.
     * If a file was added in {@code head}, then {@link FileEntity#getName} and {@link FileEntity#getContent}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    default Flux<DiffEntry> getDiff(CommitSnapshot base, CommitSnapshot head) {
        return getDiff(base, head, FileFilter.ALL);
    }

    /**
     * Get diff between two commits as {@link DiffEntry} of file entities that satisfy the filter,
     * each representing two corresponding files in {@code base} and {@code head} commit snapshots.
     * If a file was added in {@code head}, then {@link FileEntity#getName} and {@link FileEntity#getContent}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    Flux<DiffEntry> getDiff(CommitSnapshot base, CommitSnapshot head, FileFilter filter);

    /**
     * Get diff between two commits of remote repositories.
     *
     * @see #getDiff(CommitSnapshot, CommitSnapshot, FileFilter)
     */
    Flux<DiffEntry> getRemoteDiff(GithubRepo projectRepo, CommitSnapshot base, CommitSnapshot head, FileFilter filter);
}
