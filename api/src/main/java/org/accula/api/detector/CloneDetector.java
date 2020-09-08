package org.accula.api.detector;

import lombok.Builder;
import lombok.Value;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.CommitSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.function.Supplier;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CloneDetector {
    Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(CommitSnapshot commitSnapshot, Flux<FileEntity> files);

    Mono<Void> fill(Flux<FileEntity> files);

    interface ConfigProvider extends Supplier<Mono<Config>> {
        @Override
        Mono<Config> get();
    }

    @Builder
    @Value
    class Config {
        int minCloneLength;
        @Builder.Default
        FileFilter filter = FileFilter.SRC_JAVA;
    }
}
