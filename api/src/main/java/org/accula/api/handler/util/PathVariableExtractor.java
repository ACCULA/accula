package org.accula.api.handler.util;

import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * @author Anton Lamtev
 */
public final class PathVariableExtractor {
    private static final String PROJECT_ID = "projectId";
    private static final String PULL_NUMBER = "pullNumber";

    private PathVariableExtractor () {
    }

    public static Long projectId(final ServerRequest request) {
        return Long.valueOf(request.pathVariable(PROJECT_ID));
    }

    public static Integer pullNumber(final ServerRequest request) {
        return Integer.valueOf(request.pathVariable(PULL_NUMBER));
    }
}
