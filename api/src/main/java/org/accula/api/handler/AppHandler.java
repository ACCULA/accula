package org.accula.api.handler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.User;
import org.accula.api.db.model.User.Role;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.handler.dto.AppSettingsDto;
import org.accula.api.handler.dto.validation.InputDtoValidator;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class AppHandler implements Handler {
    @Getter
    private final CurrentUserRepo currentUserRepo;
    private final UserRepo userRepo;
    @Getter
    private final InputDtoValidator validator;

    public Mono<ServerResponse> settings(final ServerRequest request) {
        return checkRootRole()
            .then(userRepo.findAll().collectList())
            .map(AppHandler::toSettingsDto)
            .flatMap(Responses::ok)
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> updateSettings(final ServerRequest request) {
        return checkRootRole()
            .then(request.bodyToMono(AppSettingsDto.class))
            .doOnNext(this::validate)
            .flatMap(settings -> userRepo.setAdminRole(settings.admins()))
            .map(AppHandler::toSettingsDto)
            .flatMap(Responses::created)
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private static AppSettingsDto toSettingsDto(final List<User> allUsers) {
        final var roots = allUsers
            .stream()
            .filter(Role.ROOT::is)
            .map(User::id)
            .collect(Collectors.toSet());
        final var admins = allUsers
            .stream()
            .filter(Role.ADMIN::is)
            .map(User::id)
            .toList();
        final var users = allUsers
            .stream()
            .filter(user -> !roots.contains(user.id()))
            .map(ModelToDtoConverter::convert)
            .toList();
        return new AppSettingsDto(users, roots, admins);
    }
}
