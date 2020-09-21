package org.accula.api.token;

import org.accula.api.code.FileEntity;
import org.accula.api.token.java.JavaTokenProvider;
import reactor.core.publisher.Flux;

import java.util.List;

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

    Flux<List<Token<Ref>>> tokensByMethods(Flux<FileEntity<Ref>> files);

    enum Language {
        JAVA,
    }
}
