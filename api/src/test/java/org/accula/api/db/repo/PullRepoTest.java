package org.accula.api.db.repo;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static org.accula.api.util.TestData.highload19_174;
import static org.accula.api.util.TestData.highload2019_174Base;
import static org.accula.api.util.TestData.highload2019_174Head;
import static org.accula.api.util.TestData.user1Github;
import static org.accula.api.util.TestData.user2Github;
import static org.accula.api.util.TestData.userGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
final class PullRepoTest extends BaseRepoTest {
    private PullRepo pullRepo;
    private SnapshotRepo snapshotRepo;
    private GithubUserRepo githubUserRepo;
    private GithubRepoRepo repoRepo;
    private ProjectRepo projectRepo;
    private UserRepo userRepo;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        pullRepo = new PullRepoImpl(connectionProvider());
        snapshotRepo = new SnapshotRepoImpl(connectionProvider());
        githubUserRepo = new GithubUserRepoImpl(connectionProvider());
        repoRepo = new GithubRepoRepoImpl(connectionProvider());
        projectRepo = new ProjectRepoImpl(connectionProvider());
        userRepo = new UserRepoImpl(connectionProvider());
    }

    @Test
    void testUpsert() {
        expectCompleteEmpty(pullRepo.upsert(List.of()));

        final var head = upsertSnapshot(highload2019_174Head);
        final var base = upsertSnapshot(highload2019_174Base);
        final var pull = highload19_174.toBuilder()
            .head(head)
            .base(base)
            .build();

        pullRepo.upsert(pull)
            .as(StepVerifier::create)
            .expectNext(pull)
            .verifyComplete();
    }

    @Test
    void testFindById() {
        expectCompleteEmpty(pullRepo.findById(List.of(highload19_174.id())));

        final var pull = upsertPulls(List.of(highload19_174)).get(0);
        pullRepo
            .findById(pull.id())
            .as(StepVerifier::create)
            .expectNext(pull)
            .verifyComplete();
    }

    @Test
    void testFindByIds() {
        expectCompleteEmpty(pullRepo.findById(List.of()));

        final var pulls = new ArrayList<>(upsertPulls(TestData.pulls));
        for (int i = 0; i < 10; ++i) {
            Collections.shuffle(pulls);

            pullRepo
                .findById(pulls.stream().map(Pull::id).toList())
                .collectList()
                .as(StepVerifier::create)
                .expectNext(pulls)
                .verifyComplete();
        }
    }

    @Test
    void testFindByNumber() {
        expectCompleteEmpty(pullRepo.findByNumber(1L, 1));

        final var pulls = upsertPulls(TestData.pulls)
            .stream()
            .filter(pull -> pull.primaryProjectId() != null)
            .limit(5)
            .toList();

        for (final var pull : pulls) {
            assertNotNull(pull.primaryProjectId());
            pullRepo
                .findByNumber(pull.primaryProjectId(), pull.number())
                .as(StepVerifier::create)
                .expectNext(pull)
                .verifyComplete();
        }
    }

    @Test
    void testFindPrevious() {
        expectCompleteEmpty(pullRepo.findPrevious(1L, 1, 1L));
    }

    @Test
    void testFindByProjectId() {
        final var projectId = 1L;

        expectCompleteEmpty(pullRepo.findByProjectId(projectId));

        final var pulls = upsertPulls(TestData.pulls);
        final var expected = pulls
            .stream()
            .filter(pull -> Objects.equals(pull.primaryProjectId(), projectId))
            .collect(Collectors.toSet());
        pullRepo
            .findByProjectId(projectId)
            .collect(Collectors.toSet())
            .as(StepVerifier::create)
            .expectNext(expected)
            .verifyComplete();
    }

    @Test
    void testFindByProjectIdIncludingAssignees() {
        final var projectId = 1L;

        expectCompleteEmpty(pullRepo.findByProjectIdIncludingAssignees(projectId));

        final var pulls = upsertPulls(TestData.pulls);
        final var expected = pulls
            .stream()
            .filter(pull -> Objects.equals(pull.primaryProjectId(), projectId))
            .map(Pull::assignees)
            .collect(Collectors.toSet());
        assertFalse(expected.isEmpty());
        assertTrue(expected.stream().anyMatch(not(List::isEmpty)));
        pullRepo
            .findByProjectIdIncludingAssignees(projectId)
            .map(Pull::assignees)
            .collect(Collectors.toSet())
            .as(StepVerifier::create)
            .expectNext(expected)
            .verifyComplete();
    }

    @Test
    void testFindByProjectIdIncludingSecondaryRepos() {
        final var projectId = 1L;

        expectCompleteEmpty(pullRepo.findByProjectIdIncludingSecondaryRepos(projectId));

        final var expected = new HashSet<>(upsertPulls(TestData.pulls));
        pullRepo
            .findByProjectIdIncludingSecondaryRepos(projectId)
            .collect(Collectors.toSet())
            .as(StepVerifier::create)
            .expectNext(expected)
            .verifyComplete();
    }

    private List<Pull> upsertPulls(final Collection<Pull> pulls) {
        final var firstPull = pulls.iterator().next();
        return pulls
            .stream()
            .map(pull -> {
                final var head = upsertSnapshot(pull.head());
                final var base = upsertSnapshot(pull.base());
                final var assignees = firstPull.equals(pull) ? Stream
                    .of(userGithub, user1Github, user2Github)
                    .map(this::upsertGithubUser)
                    .toList() : List.<GithubUser>of();
                final var pullToUpsert = pull.toBuilder()
                    .head(head)
                    .base(base)
                    .assignees(assignees)
                    .build();
                final var upserted = pullRepo.upsert(pullToUpsert).block();
                assertEquals(pullToUpsert, upserted);

                TestData.projects
                    .stream()
                    .filter(project -> project.id().equals(pull.primaryProjectId()))
                    .findFirst()
                    .ifPresent(project -> {
                        final var creator = userRepo.upsert(project.creator()).block();
                        assertNotNull(creator);
                        final var proj = projectRepo.upsert(project.githubRepo(), creator).block();
                        assertNotNull(proj);
                        final var secondaryRepoIds = TestData.projects
                            .stream()
                            .map(Project::githubRepo)
                            .filter(r -> !r.equals(proj.githubRepo()))
                            .map(this::upsertRepo)
                            .map(GithubRepo::id)
                            .filter(id -> !id.equals(proj.githubRepo().id()))
                            .toList();
                        projectRepo.attachRepos(proj.id(), secondaryRepoIds).block();
                    });

                return upserted;
            })
            .toList();
    }

    private Snapshot upsertSnapshot(final Snapshot snapshot) {
        upsertRepo(snapshot.repo());
        final var upserted = snapshotRepo.insert(snapshot).block();
        assertNotNull(upserted);
        return upserted;
    }

    private GithubRepo upsertRepo(final GithubRepo repo) {
        final var owner = githubUserRepo.upsert(repo.owner()).block();
        assertNotNull(owner);
        final var upserted = repoRepo.upsert(repo).block();
        assertNotNull(upserted);
        return upserted;
    }

    private GithubUser upsertGithubUser(final GithubUser user) {
        final var upserted = githubUserRepo.upsert(user).block();
        assertNotNull(upserted);
        return upserted;
    }
}
