package org.accula.api.code;

import org.accula.api.db.model.CommitSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public final class GitCliCodeLoader implements CodeLoader{
    @Override
    public Flux<FileEntity> getFiles(final CommitSnapshot snapshot) {
        return Flux.empty();
    }

    @Override
    public Flux<FileEntity> getFiles(final CommitSnapshot snapshot, final FileFilter filter) {
        return Flux.empty();
    }

    @Override
    public Mono<FileEntity> getFileSnippet(final CommitSnapshot snapshot, final String filename, final int fromLine, final int toLine) {
        return Mono.empty();
    }

    @Override
    public Flux<Tuple2<FileEntity, FileEntity>> getDiff(final CommitSnapshot base, final CommitSnapshot head) {
        return Flux.empty();
    }

    @Override
    public Flux<Tuple2<FileEntity, FileEntity>> getDiff(final CommitSnapshot base, final CommitSnapshot head, final FileFilter filter) {
        return Flux.empty();
    }

    @Override
    public Flux<Tuple2<FileEntity, FileEntity>> getRemoteDiff(final CommitSnapshot origin, final CommitSnapshot remote, final FileFilter filter) {
        return Flux.empty();
    }
}
