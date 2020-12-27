package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
@JsonInclude(NON_NULL)
public class ApiError {
    String code;
    @Nullable
    String description;

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
