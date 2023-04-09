package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
public record ApiError(String code, @Nullable String description) {
    public static ApiError with(final Code code) {
        return new ApiError(code.stringRepresentation(), null);
    }

    public static ApiError withDescription(final String description, final Code code) {
        return new ApiError(code.stringRepresentation(), description);
    }

    public interface Code {
        default String stringRepresentation() {
            return toString();
        }
    }
}
