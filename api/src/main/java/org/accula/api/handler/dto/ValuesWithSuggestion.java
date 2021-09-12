package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
public record ValuesWithSuggestion<Value, Suggested>(
    List<Value> value,
    @OptionalField @Nullable List<Suggested> suggestion
) implements InputDto {
    public ValuesWithSuggestion(final Value value) {
        this(List.of(value), List.of());
    }

    public ValuesWithSuggestion(final List<Value> values) {
        this(values, List.of());
    }
}
