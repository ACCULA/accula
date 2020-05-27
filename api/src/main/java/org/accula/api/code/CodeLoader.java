package org.accula.api.code;

import org.accula.api.db.model.Commit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 */
public interface CodeLoader {
    /**
     * Get all file entities by the commit
     */
    Flux<FileEntity> getFiles(Commit commit);

    /**
     * Get all file entities that satisfies the filter by the commit
     */
    Flux<FileEntity> getFiles(Commit commit, FileFilter filter);

    /**
     * Get the file content by the commit and the file name
     */
    Mono<String> getFile(Commit commit, String filename);

    /**
     * Get the file snippet (file content from the specified line range)
     * by the commit, the file name and the line range
     */
    Mono<String> getFileSnippet(Commit commit, String filename, int fromLine, int toLine);
}
