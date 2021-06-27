package org.accula.api.token;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.FileEntity;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
@Slf4j
public final class TokenProvider<Ref> {
    private final Collection<LanguageTokenProvider<Ref>> tokenProviders;

    public TokenProvider(final Collection<LanguageTokenProvider<Ref>> tokenProviders) {
        if (tokenProviders.isEmpty()) {
            throw new IllegalArgumentException("tokenProviders MUST NOT be empty");
        }
        this.tokenProviders = tokenProviders;
    }

    public Flux<List<Token<Ref>>> tokensByMethods(final Flux<FileEntity<Ref>> files) {
        return files
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(file -> {
                final var tokenProvider = tokenProvider(file);
                if (tokenProvider == null) {
                    log.warn("No tokenProvider found for file {}", file);
                    return Mono.empty();
                }
                return Flux.fromStream(tokenProvider.tokensByMethods(file));
            })
            .sequential();
    }

    @Nullable
    private LanguageTokenProvider<Ref> tokenProvider(final FileEntity<Ref> file) {
        for (final var tokenProvider : tokenProviders) {
            if (tokenProvider.supportsFile(file)) {
                return tokenProvider;
            }
        }
        return null;
    }
}
