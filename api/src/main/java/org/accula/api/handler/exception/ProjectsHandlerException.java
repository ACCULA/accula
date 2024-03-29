package org.accula.api.handler.exception;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.util.Responses;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.Serial;
import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public final class ProjectsHandlerException extends ResponseConvertibleException {
    @Serial
    private static final long serialVersionUID = 2418056639476069599L;

    private ProjectsHandlerException(final Error error, @Nullable final String description) {
        super(error, description);
    }

    public static ProjectsHandlerException invalidUrl(final String url) {
        return new ProjectsHandlerException(Error.INVALID_URL, "Invalid url: " + url);
    }

    public static ProjectsHandlerException alreadyExists(final GithubRepo.Identity repoIdentity) {
        return new ProjectsHandlerException(Error.ALREADY_EXISTS, "Repo %s already exists".formatted(repoIdentity));
    }

    public static ProjectsHandlerException unableRetrieveGithubRepo(final String owner, final String repo) {
        return new ProjectsHandlerException(Error.UNABLE_RETRIEVE_GITHUB_REPO,
                "Unable to retrieve github repo %s/%s".formatted(owner, repo));
    }

    public static ProjectsHandlerException noPermission() {
        return new ProjectsHandlerException(Error.NO_PERMISSION, null);
    }

    @Override
    public Function<Object, Mono<ServerResponse>> responseFunctionForCode(final ApiError.Code code) {
        return switch ((Error) code) {
            case INVALID_URL, UNABLE_RETRIEVE_GITHUB_REPO -> Responses::badRequest;
            case NO_PERMISSION -> Responses::forbidden;
            case ALREADY_EXISTS -> Responses::conflict;
        };
    }

    @Override
    public boolean needsResponseBody() {
        return true;
    }

    private enum Error implements ApiError.Code {
        INVALID_URL,
        ALREADY_EXISTS,
        UNABLE_RETRIEVE_GITHUB_REPO,
        NO_PERMISSION,
    }
}
