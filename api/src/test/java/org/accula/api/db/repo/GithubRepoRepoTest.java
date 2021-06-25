package org.accula.api.db.repo;

import org.accula.api.db.model.GithubRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.accula.api.util.TestData.acculaAccula;
import static org.accula.api.util.TestData.polisHighload2019;
import static org.accula.api.util.TestData.repos;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Anton Lamtev
 */
final class GithubRepoRepoTest extends BaseRepoTest {
    private GithubRepoRepo repoRepo;
    private GithubUserRepo userRepo;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        repoRepo = new GithubRepoRepoImpl(connectionProvider());
        userRepo = new GithubUserRepoImpl(connectionProvider());
    }

    @Test
    void testUpsert() {
        expectCompleteEmpty(repoRepo.upsert(List.of()));

        assertNotNull(userRepo.upsert(repos.stream().map(GithubRepo::owner).toList()).collectList().block());

        StepVerifier.create(repoRepo.upsert(repos))
            .expectNext(repos.toArray(GithubRepo[]::new))
            .expectComplete()
            .verify();

        StepVerifier.create(repoRepo.upsert(acculaAccula))
            .expectNext(acculaAccula)
            .expectComplete()
            .verify();
    }

    @Test
    void testFindById() {
        expectCompleteEmpty(repoRepo.findById(polisHighload2019.id()));

        assertNotNull(userRepo.upsert(polisHighload2019.owner()).block());
        assertNotNull(repoRepo.upsert(polisHighload2019).block());

        StepVerifier.create(repoRepo.findById(polisHighload2019.id()))
            .expectNext(polisHighload2019)
            .expectComplete()
            .verify();
    }

    @Test
    void testFindByName() {
        expectCompleteEmpty(repoRepo.findByName(acculaAccula.owner().login(), acculaAccula.name()));

        assertNotNull(userRepo.upsert(acculaAccula.owner()).block());
        assertNotNull(repoRepo.upsert(acculaAccula).block());

        StepVerifier.create(repoRepo.findByName(acculaAccula.owner().login(), acculaAccula.name()))
            .expectNext(acculaAccula)
            .expectComplete()
            .verify();
    }
}
