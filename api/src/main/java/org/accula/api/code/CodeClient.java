package org.accula.api.code;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 */
public interface CodeClient {
    /**
     * Get all file entities
     * by the owner/repo pair and the commit SHA
     */
    Flux<FileEntity> getFiles(CommitMarker marker);

    /**
     * Get all file entities that satisfies the filter
     * by the owner/repo pair and the commit SHA
     */
    Flux<FileEntity> getFiles(CommitMarker marker, FileFilter filter);

    /**
     * Get the file content
     * by the owner/repo pair, commit SHA and file name
     */
    Mono<String> getFile(CommitMarker marker, String fileName);

    /**
     * Get the file snippet (file content from the specified line range)
     * by the owner/repo pair, commit SHA, file name and the line range
     */
    Mono<String> getFileSnippet(CommitMarker marker, String fileName, int fromLine, int toLine);
}
