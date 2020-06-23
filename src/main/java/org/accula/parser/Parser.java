package org.accula.parser;

import generated.org.accula.parser.Java9Lexer;
import generated.org.accula.parser.Java9Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import com.suhininalex.clones.core.structures.Token;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Parser {
    public static List<List<Token>> getFunctionsAsTokens(@NotNull final File file) {
        final var lexer = new Java9Lexer(CharStreams.fromString(file.getContent()));
        final var tokens = new CommonTokenStream(lexer);
        final var parser = new Java9Parser(tokens);
        final var parseTree = parser.compilationUnit();

        final var walker = new ParseTreeWalker();
        final var listener = new JavaListener(tokens);
        walker.walk(listener, parseTree);

        // Exclude braces, semicolons, whitespaces, comments, ...
        final var excludeTokens = Set.of(68, 69, 70, 71, 74, 116, 117, 118);

        return listener
                .getFunctions()
                .stream()
                .map(func -> func
                        .stream()
                        .filter(token -> !excludeTokens.contains(token.getType()))
                        .map(token -> new Token(
                                token.getType(),
                                token.getText(),
                                token.getLine(),
                                file.getName(),
                                file.getOwner(),
                                file.getPath())
                        )
                        .collect(Collectors.toUnmodifiableList())
                )
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<Token> getTokenizedString(@NotNull final String str) {
        return str
                .chars()
                .mapToObj(i -> new Token(
                        ThreadLocalRandom.current().nextInt(),
                        String.valueOf((char) i),
                        ThreadLocalRandom.current().nextInt(),
                        "None" + ThreadLocalRandom.current().nextInt(),
                        "None" + ThreadLocalRandom.current().nextInt(),
                        "None" + ThreadLocalRandom.current().nextInt())
                )
                .collect(Collectors.toUnmodifiableList());
    }
}
