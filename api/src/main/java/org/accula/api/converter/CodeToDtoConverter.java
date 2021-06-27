package org.accula.api.converter;

import org.accula.api.code.DiffEntry;
import org.accula.api.db.model.Snapshot;
import org.accula.api.handler.dto.DiffDto;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Anton Lamtev
 */
public final class CodeToDtoConverter {
    private CodeToDtoConverter() {
    }

    public static DiffDto convert(final DiffEntry<Snapshot> diff) {
        final var base = diff.base();
        final var head = diff.head();
        return DiffDto.builder()
                .baseFilename(nullIfEmpty(base.name()))
                .baseContent(encode(base.content()))
                .headFilename(nullIfEmpty(head.name()))
                .headContent(encode(head.content()))
                .build();
    }

    @Nullable
    private static String encode(@Nullable final String text) {
        if (text == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    private static String nullIfEmpty(@Nullable final String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        return string;
    }
}
