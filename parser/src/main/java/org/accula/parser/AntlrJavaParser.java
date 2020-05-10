package org.accula.parser;

import generated.org.accula.parser.Java9Lexer;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;

@Slf4j
public class AntlrJavaParser implements Parser<Token> {
    public Flux<Token> getTokens(final InputStream file) {
        return Mono
                .fromCallable(() -> {
                    var lexer = new Java9Lexer(CharStreams.fromStream(file));
                    var tokens = new CommonTokenStream(lexer);
                    tokens.fill();
                    return tokens.getTokens();
                })
                .doOnError(e -> log.error("Error parsing file: {}", e.getMessage()))
                .flatMapMany(Flux::fromIterable);
    }
}
