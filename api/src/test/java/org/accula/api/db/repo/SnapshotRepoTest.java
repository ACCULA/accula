package org.accula.api.db.repo;

import org.accula.api.db.model.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Collectors;

import static org.accula.api.util.TestData.acculaAccula;
import static org.accula.api.util.TestData.highload19_174;
import static org.accula.api.util.TestData.highload2019_174Head;
import static org.accula.api.util.TestData.repos;
import static org.accula.api.util.TestData.snapshots;
import static org.accula.api.util.TestData.usersGithub;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Anton Lamtev
 */
final class SnapshotRepoTest extends BaseRepoTest {
    private GithubUserRepo userRepo;
    private GithubRepoRepo repoRepo;
    private SnapshotRepo snapshotRepo;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        userRepo = new GithubUserRepoImpl(connectionProvider());
        repoRepo = new GithubRepoRepoImpl(connectionProvider());
        snapshotRepo = new SnapshotRepoImpl(connectionProvider());
    }

    @Test
    void testInsert() {
        expectCompleteEmpty(snapshotRepo.insert(List.of()));

        assertNotNull(userRepo.upsert(usersGithub).collectList().block());
        assertNotNull(repoRepo.upsert(repos).collectList().block());

        StepVerifier.create(snapshotRepo.insert(snapshots))
            .expectNext(snapshots.toArray(Snapshot[]::new))
            .verifyComplete();

        snapshotRepo.insert(highload2019_174Head)
            .as(StepVerifier::create)
            .expectNext(highload2019_174Head)
            .verifyComplete();
    }

    @Test
    void testFindById() {
        expectCompleteEmpty(snapshotRepo.findById(List.of()));

        assertNotNull(userRepo.upsert(usersGithub).collectList().block());
        assertNotNull(repoRepo.upsert(repos).collectList().block());
        assertNotNull(snapshotRepo.insert(snapshots).collectList().block());

        StepVerifier.create(snapshotRepo.findById(snapshots.stream().map(Snapshot::id).toList()).collect(Collectors.toSet()))
            .expectNextMatches(retrievedSnapshots -> retrievedSnapshots.containsAll(snapshots))
            .expectComplete()
            .verify();

        StepVerifier.create(snapshotRepo.findById(highload2019_174Head.id()))
            .expectNext(highload2019_174Head)
            .verifyComplete();
    }

    @Test
    void testFindByRepoId() {
        expectCompleteEmpty(snapshotRepo.findByRepoId(acculaAccula.id()));

        assertNotNull(userRepo.upsert(usersGithub).collectList().block());
        assertNotNull(repoRepo.upsert(repos).collectList().block());
        assertNotNull(snapshotRepo.insert(snapshots).collectList().block());

        final var acculaSnapshots = snapshots
            .stream()
            .filter(s -> s.repo().equals(acculaAccula))
            .collect(Collectors.toSet());
        StepVerifier.create(snapshotRepo.findByRepoId(acculaAccula.id()).collect(Collectors.toSet()))
            .expectNext(acculaSnapshots)
            .verifyComplete();
    }

    @Test
    void testFindByPullId() {
        expectCompleteEmpty(snapshotRepo.findByPullId(highload19_174.id()));
        //TODO:
    }
}
