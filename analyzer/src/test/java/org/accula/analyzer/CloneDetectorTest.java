package org.accula.analyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DisplayName("Clone detector tests")
class CloneDetectorTest {
    @Test
    @DisplayName("Test with empty data")
    void analyzeTestEmptyInputData() {
        var detector = new CloneDetector();
        StepVerifier.create(
                detector.analyze(Flux.empty(), 1.0f, 10))
                .verifyComplete();
    }
}
