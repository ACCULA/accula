package org.accula.api.handler.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

/**
 * @author Anton Lamtev
 */
@RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
public final class ErrorBody implements ResponseBody {
    private final String error;
}
