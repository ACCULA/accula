package org.accula.api.clone;

import lombok.Builder;
import lombok.Value;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Snapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CloneDetector {
    Flux<CodeClone> readClones(Snapshot pullSnapshot);

    Flux<CodeClone> findClones(Snapshot pullSnapshot, Flux<FileEntity<Snapshot>> files);

    Mono<Void> fill(Flux<FileEntity<Snapshot>> files);

    interface ConfigProvider extends Supplier<Mono<Config>> {
        @Override
        Mono<Config> get();
    }

    @Builder
    @Value
    class Config {
        int cloneMinTokenCount;
        @Builder.Default
        FileFilter filter = FileFilter.SRC_JAVA;
    }
}
