package org.accula.analyzer.checkers.utils;

import org.accula.analyzer.File;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataTransformer {
    public static <T> File<T> convertString(final File<String> file,
                                            final Function<InputStream, T> transformer) {
        return new File<T>(
                file.getName(),
                file.getPath(),
                file.getOwner(),
                file.getRepo(),
                transformer.apply(new ByteArrayInputStream(file.getContent().getBytes(UTF_8)))
        );
    }
}
