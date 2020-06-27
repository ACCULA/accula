package org.accula.parser;

import generated.org.accula.parser.Java9Lexer;
import generated.org.accula.parser.Java9Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
    public static Stream<List<Token>> getFunctionsAsTokens(@NotNull final File file) {
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
                        .map(token -> new Token(
                                token.getType(),
                                token.getText(),
                                token.getLine(),
                                file.getName(),
                                file.getOwner(),
                                file.getPath())
                        )
                        .map(Parser::anonymize)
                        .collect(Collectors.toUnmodifiableList())
                );
    }

    private static boolean isAllowedToken(@NotNull final org.antlr.v4.runtime.Token token,
                                          @NotNull final Set<org.antlr.v4.runtime.Token> typeArgs) {
        final var excludeTokens = Set.of(
                Java9Lexer.LPAREN,
                Java9Lexer.RPAREN,
                Java9Lexer.LBRACE,
                Java9Lexer.RBRACE,
                Java9Lexer.SEMI,
                Java9Lexer.WS,
                Java9Lexer.COMMENT,
                Java9Lexer.LINE_COMMENT,
                Java9Lexer.FINAL
        );
        return !excludeTokens.contains(token.getType()) && !typeArgs.contains(token);
    }

    private static Token anonymize(@NotNull final Token token) {
        switch (token.getType()) {
            case Java9Lexer.Identifier -> token.setText("id");
            case Java9Lexer.IntegerLiteral -> token.setText("int");
            case Java9Lexer.FloatingPointLiteral -> token.setText("float");
            case Java9Lexer.BooleanLiteral -> token.setText("bool");
            case Java9Lexer.CharacterLiteral -> token.setText("char");
            case Java9Lexer.StringLiteral -> token.setText("string");
        }
        return token;
    }
}
