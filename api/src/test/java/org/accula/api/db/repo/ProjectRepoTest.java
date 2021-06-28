package org.accula.api.db.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.accula.api.db.model.CodeLanguage.JAVA;
import static org.accula.api.db.model.CodeLanguage.KOTLIN;

/**
 * @author Anton Lamtev
 */
final class ProjectRepoTest extends BaseRepoTest {
    private ProjectRepo projectRepo;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        projectRepo = new ProjectRepoImpl(connectionProvider());
    }

    @Test
    void testAllDetectorLanguages() {
        StepVerifier.create(projectRepo.supportedLanguages())
            .expectNext(List.of(JAVA, KOTLIN))
            .verifyComplete();
    }
}
