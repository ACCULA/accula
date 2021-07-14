package org.accula.api.db.repo;

import org.accula.api.db.model.GithubUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.accula.api.util.TestData.lamtevGithub;
import static org.accula.api.util.TestData.polisGithub;
import static org.accula.api.util.TestData.usersGithub;
import static org.accula.api.util.TestData.vaddyaGithub;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Anton Lamtev
 */
final class GithubUserRepoTest extends BaseRepoTest {
    private GithubUserRepo userRepo;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        userRepo = new GithubUserRepoImpl(connectionProvider());
    }

    @Test
    void testUpsert() {
        expectCompleteEmpty(userRepo.upsert(List.of()));

        StepVerifier.create(userRepo.upsert(usersGithub))
            .expectNext(usersGithub.toArray(GithubUser[]::new))
            .verifyComplete();

        StepVerifier.create(userRepo.upsert(polisGithub))
            .expectNext(polisGithub)
            .verifyComplete();
    }

    @Test
    void testFindById() {
        expectCompleteEmpty(userRepo.findById(lamtevGithub.id()));

        assertNotNull(userRepo.upsert(vaddyaGithub).block());

        StepVerifier.create(userRepo.findById(vaddyaGithub.id()))
            .expectNext(vaddyaGithub)
            .verifyComplete();
    }
}
