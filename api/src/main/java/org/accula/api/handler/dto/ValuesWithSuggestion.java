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

    public static Builder<?, ?> builder() {
        return new Builder<>();
    }

    public static final class Builder<Value, Suggested> {
        private List<Value> values = List.of();
        private List<Suggested> suggestion = List.of();

        private Builder() {
        }

        @SuppressWarnings("unchecked")
        public <V> Builder<V, ?> values(final List<V> values) {
            this.values = (List<Value>) values;
            return (Builder<V, ?>) this;
        }

        @SuppressWarnings("unchecked")
        public <S> Builder<?, S> suggestion(final List<S> suggestion) {
            this.suggestion = (List<Suggested>) suggestion;
            return (Builder<?, S>) this;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public <V, S> ValuesWithSuggestion<V, S> build() {
            return new ValuesWithSuggestion(values, suggestion);
        }
    }
}
