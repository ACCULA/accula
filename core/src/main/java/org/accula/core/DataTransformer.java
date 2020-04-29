package org.accula.core;

import org.accula.data.model.GFile;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;

public class DataTransformer {
    @NotNull
    public static <T> GFile<T> transformContent(@NotNull final GFile<String> gFile,
                                                @NotNull final Function<InputStream, Optional<T>> transformer) {
        return new GFile<>(gFile.userName(),
                gFile.prNumber(),
                gFile.fileName(),
                gFile.link(),
                transformer.apply(new ByteArrayInputStream(gFile.content().getBytes()))
                        .orElseThrow());
    }
}
