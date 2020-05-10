package org.accula.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@DisplayName("ANTLR parser tests")
class AntlrJavaParserTest {
    @Test
    @DisplayName("Test with empty input")
    void tokensTestEmpty() {
        final var tokenizer = new AntlrJavaParser();
        StepVerifier.create(
                tokenizer.getTokens(new ByteArrayInputStream("".getBytes(UTF_8))))
                .expectNextCount(1)
                .verifyComplete();

    }

    @Test
    @DisplayName("Test with simple input")
    void tokensTest() {
        final var tokenizer = new AntlrJavaParser();
        StepVerifier.create(
                tokenizer.getTokens(
                        new ByteArrayInputStream("package org.accula.parser;".getBytes(UTF_8))))
                .expectNextCount(8)
                .verifyComplete();
    }
}
