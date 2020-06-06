package org.accula.api.code;

import org.accula.api.db.model.CommitOld;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * @author Vadim Dyachkov
 */
public interface CodeLoader {
    /**
     * Get all file entities by the commit
     */
    Flux<FileEntity> getFiles(CommitOld commit);

    /**
     * Get all file entities that satisfies the filter by the commit
     */
    Flux<FileEntity> getFiles(CommitOld commit, FileFilter filter);

    /**
     * Get the file content by the commit and the file name
     */
    Mono<FileEntity> getFile(CommitOld commit, String filename);

    /**
     * Get the file snippet (file entity with content from the specified line range)
     * by the commit, the file name and the line range
     */
    Mono<FileEntity> getFileSnippet(CommitOld commit, String filename, int fromLine, int toLine);

    /**
     * Get diff between two commits as tuples of file entities,
     * each representing two corresponding files in {@code base} and {@code head} commits.
     * If a file was added in {@code head}, then {@link FileEntity#getName} and {@link FileEntity#getContent}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    Flux<Tuple2<FileEntity, FileEntity>> getDiff(CommitOld base, CommitOld head);

    /**
     * Get diff between two commits as tuples of file entities that satisfy the filter,
     * each representing two corresponding files in {@code base} and {@code head} commits.
     * If a file was added in {@code head}, then {@link FileEntity#getName} and {@link FileEntity#getContent}
     * of the first element of the tuple return {@code null}.
     * If file was removed in {@code head}, then second tuple element values are equal to {@code null}.
     */
    Flux<Tuple2<FileEntity, FileEntity>> getDiff(CommitOld base, CommitOld head, FileFilter filter);
}
