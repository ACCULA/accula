package org.accula.api.token;

import org.accula.api.code.FileEntity;
import org.accula.api.token.java.JavaTokenProvider;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public interface TokenProvider<Ref> {
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    static <Ref> TokenProvider<Ref> of(final Language language) {
        return switch (language) {
            case JAVA -> new JavaTokenProvider<>();
        };
    }

    Flux<Stream<Token<Ref>>> tokensByMethods(Flux<FileEntity<Ref>> files);

    enum Language {
        JAVA,
    }
}
