package org.accula.core;

import lombok.extern.slf4j.Slf4j;
import org.accula.parser.Java9Lexer;
import org.accula.parser.Java9Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
public class ParserUtils {
    @NotNull
    public static Optional<ParseTree> getAST(@NotNull final InputStream file) {
        try {
            var charStream = CharStreams.fromStream(file);
            var lexer = new Java9Lexer(charStream);
            var tokens = new CommonTokenStream(lexer);
            var parser = new Java9Parser(tokens);
            // Try to reduce memory consumption -> low performance ? -Xmx2g ?
            // https://github.com/antlr/antlr4/issues/1944
            return Optional.of(parser.compilationUnit());

        } catch (IOException e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }
}
