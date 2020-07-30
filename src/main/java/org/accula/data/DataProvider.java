package org.accula.data;

import org.accula.parser.FileEntity;
import reactor.core.publisher.Flux;

@FunctionalInterface
public interface DataProvider {
    Flux<FileEntity> getFiles();
}
