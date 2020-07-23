package org.accula.api.detector.parser;

import generated.Java9Lexer;
import generated.Java9Parser;
import org.accula.api.code.FileEntity;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.suhininalex.clones.core.structures.Token;

public final class Parser {
    private Parser() {

    }

    public static Stream<List<Token>> tokenizedFunctions(final FileEntity file) {
        final var lexer = new Java9Lexer(CharStreams.fromString(file.getContent()));
        final var tokens = new CommonTokenStream(lexer);
        final var parser = new Java9Parser(tokens);
        final var parseTree = parser.compilationUnit();

        final var walker = new ParseTreeWalker();
        final var listener = new JavaListener(tokens);
        walker.walk(listener, parseTree);

        return listener
                .getFunctions()
                .map(func -> func
                        .stream()
                        .filter(token -> isAllowedToken(token, listener.getTypeArgs()))
                        .map(token -> anonymize(token, file))
                        .collect(Collectors.toUnmodifiableList()));
    }

    private static boolean isAllowedToken(final org.antlr.v4.runtime.Token token,
                                          final Set<org.antlr.v4.runtime.Token> typeArgs) {
        return !TokenFilter.EXCLUDE_TOKENS.contains(token.getType())
                && !typeArgs.contains(token);
    }

    private static Token anonymize(final org.antlr.v4.runtime.Token antlrToken, final FileEntity file) {
        //@formatter:off
        Token token = new Token(antlrToken.getType(),
                                antlrToken.getText(),
                                antlrToken.getLine(),
                                file.getName(),
                                file.getCommitSnapshot());
        //@formatter:on

        if (TokenFilter.PRIMITIVE_TYPES.contains(token.getType())) {
            token.setType(Java9Lexer.Identifier);
        }
        return token;
    }
}
