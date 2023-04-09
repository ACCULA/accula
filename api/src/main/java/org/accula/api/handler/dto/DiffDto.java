package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Vadim Dyachkov
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
public record DiffDto(
    @Nullable String baseFilename,
    @Nullable String headFilename,
    @Nullable String baseContent,
    @Nullable String headContent
) {
}
