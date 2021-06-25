package org.accula.api.util;

import lombok.SneakyThrows;
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.exception.ResponseConvertibleException;

/**
 * @author Anton Lamtev
 */
public final class ApiErrors {
    @SneakyThrows
    public static ApiError toApiError(ResponseConvertibleException e) {
        final var toApiError = ResponseConvertibleException.class.getDeclaredMethod("toApiError");
        toApiError.setAccessible(true);
        return (ApiError) toApiError.invoke(e);
    }
}
