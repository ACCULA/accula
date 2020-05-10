package org.accula.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

class AntlrJavaParserTest {
    @Test
    @DisplayName("Test with empty input")
    void getTokensTestEmpty() {
        var tokenizer = new AntlrJavaParser();
        StepVerifier.create(
                tokenizer.getTokens(new ByteArrayInputStream("".getBytes(UTF_8))))
                .verifyComplete();

    }

    @Test
    @DisplayName("Test with simple input")
    void getTokensTest() {
        var tokenizer = new AntlrJavaParser();
        StepVerifier.create(
                tokenizer.getTokens(
                        new ByteArrayInputStream("package org.accula.parser;".getBytes(UTF_8))))
                .expectNextCount(7)
                .verifyComplete();
    }
}
