package org.accula.api.code;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 */
public interface CodeLoader {
    /**
     * Get all file entities by the commit marker
     */
    Flux<FileEntity> getFiles(CommitMarker marker);

    /**
     * Get all file entities that satisfies the filter by the commit marker
     */
    Flux<FileEntity> getFiles(CommitMarker marker, FileFilter filter);

    /**
     * Get the file content by the commit marker and file name
     */
    Mono<String> getFile(CommitMarker marker, String filename);

    /**
     * Get the file snippet (file content from the specified line range)
     * by commit marker, file name and the line range
     */
    Mono<String> getFileSnippet(CommitMarker marker, String filename, int fromLine, int toLine);
}
