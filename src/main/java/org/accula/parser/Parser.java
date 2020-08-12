package org.accula.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Parser {
    private static final JavaParser parser = new JavaParser();
    private static final Range defaultRange = new Range(new Position(1, 1), new Position(1, 1));

    public static Flux<List<Token>> getFunctionsAsTokens(@NotNull final FileEntity fileEntity) {
        final var functions = new ArrayList<List<JavaToken>>();
        final var visitor = new JavaVisitor();
        parser
                .parse(fileEntity.getContent())
                .ifSuccessful(c -> c.accept(visitor, functions));

        return Flux
                .fromStream(
                        functions
                                .stream()
                                .filter(it -> !it.isEmpty())
                                .map(func -> func
                                        .stream()
                                        .filter(javaToken -> isAllowedToken(javaToken, visitor.getTypeArgs()))
                                        .map(javaToken -> anonymize(javaToken, fileEntity))
                                        .collect(Collectors.toUnmodifiableList())
                                )
                );
    }

    private static boolean isAllowedToken(@NotNull final JavaToken javaToken,
                                          @NotNull final Set<JavaToken> typeArgs) {
        return !TokenFilter.EXCLUDE_TOKENS.contains(javaToken.getKind()) && !typeArgs.contains(javaToken);
    }

    private static Token anonymize(@NotNull final JavaToken javaToken,
                                   @NotNull final FileEntity fileEntity) {
        final var type = TokenFilter.PRIMITIVE_TYPES.contains(javaToken.getKind()) ?
                JavaToken.Kind.IDENTIFIER.getKind() : javaToken.getKind();

        return new Token(
                type,
                javaToken.getText(),
                javaToken.getRange().orElse(defaultRange).begin.line,
                fileEntity.getName(),
                fileEntity.getOwner(),
                fileEntity.getPath()
        );
    }
}
