package org.accula.api.db.repo;

import org.accula.api.db.model.CodeLanguage;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.accula.api.util.TestData.highload19Project;
import static org.accula.api.util.TestData.lamtevNoIdentity;
import static org.accula.api.util.TestData.polisHighload2017;
import static org.accula.api.util.TestData.polisHighload2019;
import static org.accula.api.util.TestData.vaddyaHighload2019;
import static org.accula.api.util.TestData.vaddyaNoIdentity;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Anton Lamtev
 */
final class ProjectRepoTest extends BaseRepoTest implements ProjectRepo.OnConfUpdate {
    private ProjectRepo projectRepo;
    private UserRepo userRepo;
    private GithubUserRepo githubUserRepo;
    private GithubRepoRepo repoRepo;

    @Override
    public void onConfUpdate(final Long projectId) {
        assertNotNull(projectId);
    }

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        projectRepo = new ProjectRepoImpl(connectionProvider());
        projectRepo.addOnConfUpdate(this);
        userRepo = new UserRepoImpl(connectionProvider());
        githubUserRepo = new GithubUserRepoImpl(connectionProvider());
        repoRepo = new GithubRepoRepoImpl(connectionProvider());
    }

    @Test
    void testNotExists() {
        projectRepo
            .notExists(highload19Project.githubRepo().id())
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void testUpsert() {
        final var creator = userRepo.upsert(lamtevNoIdentity).block();
        assertNotNull(creator);
        final var owner = githubUserRepo.upsert(polisHighload2019.owner()).block();
        assertNotNull(owner);

        projectRepo.upsert(polisHighload2019, creator)
            .as(StepVerifier::create)
            .expectNextMatches(project -> project.creator().equals(creator) &&
                                          project.githubRepo().owner().equals(owner) &&
                                          project.githubRepo().name().equals(polisHighload2019.name()))
            .verifyComplete();
    }

    @Test
    void testUpdateState() {
        expectCompleteEmpty(projectRepo.updateState(highload19Project.id(), Project.State.CONFIGURED));
    }

    @Test
    void testFindById() {
        expectCompleteEmpty(projectRepo.findById(highload19Project.id()));

        final var project = upsertProject();

        projectRepo
            .findById(project.id())
            .as(StepVerifier::create)
            .expectNext(project)
            .verifyComplete();
    }

    @Test
    void testIdByRepoId() {
        expectCompleteEmpty(projectRepo.idByRepoId(highload19Project.id()));

        final var project = upsertProject();

        projectRepo
            .idByRepoId(project.githubRepo().id())
            .as(StepVerifier::create)
            .expectNext(project.id())
            .verifyComplete();
    }

    @Test
    void testGetTop() {
        expectCompleteEmpty(projectRepo.getTop(1));

        final var project = upsertProject();

        projectRepo
            .getTop(1)
            .as(StepVerifier::create)
            .expectNext(project)
            .verifyComplete();
    }

    @Test
    void testDelete() {
        projectRepo
            .delete(highload19Project.id(), highload19Project.creator().id())
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        upsertProject();

        projectRepo
            .delete(highload19Project.id(), highload19Project.creator().id())
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        expectCompleteEmpty(projectRepo.findById(highload19Project.id()));
    }

    @Test
    void testHasAdmin() {
        final var project = upsertProject();
        projectRepo
            .hasAdmin(project.id(), project.creator().id())
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void testUpsertConf() {
        final var project = upsertProject();
        final var users = upsertGithubUsers();
        final var conf = Project.Conf
            .defaultConf()
            .withLanguages(List.of(CodeLanguage.values()))
            .withExcludedSourceAuthorIds(users.stream().map(GithubUser::id).toList());
        projectRepo
            .upsertConf(project.id(), conf)
            .as(StepVerifier::create)
            .expectNext(conf)
            .verifyComplete();

        final var conf2 = conf.withExcludedSourceAuthorIds(List.of());
        projectRepo
            .upsertConf(project.id(), conf2)
            .as(StepVerifier::create)
            .expectNext(conf2)
            .verifyComplete();
    }

    @Test
    void testConfById() {
        final var project = upsertProject();
        final var admin = userRepo.upsert(vaddyaNoIdentity).block();
        assertNotNull(admin);
        final var conf = Project.Conf
            .defaultConf()
            .withLanguages(List.of(CodeLanguage.values()))
            .withAdminIds(List.of(admin.id()))
            .withExcludedSourceAuthorIds(List.of());
        assertNotNull(projectRepo.upsertConf(project.id(), conf).block());

        projectRepo
            .confById(project.id())
            .as(StepVerifier::create)
            .expectNext(conf)
            .verifyComplete();
    }

    @Test
    void testAllDetectorLanguages() {
        projectRepo
            .supportedLanguages()
            .as(StepVerifier::create)
            .expectNext(List.of(CodeLanguage.values()))
            .verifyComplete();
    }

    @Test
    void testProjectDoesNotContainRepo() {
        final var project = upsertProject();
        projectRepo
            .projectDoesNotContainRepo(project.id(), Long.MAX_VALUE)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
        projectRepo
            .projectDoesNotContainRepo(project.id(), project.githubRepo().id())
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void testAttachRepos() {
        expectCompleteEmpty(projectRepo.attachRepos(1L, List.of()));

        final var project = upsertProject();
        final var repos = upsertRepos().stream().sorted(Comparator.comparing(GithubRepo::id)).toList();
        projectRepo
            .attachRepos(project.id(), repos.stream().map(GithubRepo::id).toList())
            .as(StepVerifier::create)
            .verifyComplete();

        projectRepo
            .findById(project.id())
            .map(Project::secondaryRepos)
            .map(projectRepos -> projectRepos.stream().sorted(Comparator.comparing(GithubRepo::id)).toList())
            .as(StepVerifier::create)
            .expectNext(repos)
            .verifyComplete();
    }

    @Test
    void testFindOwnerOfProjectContainingRepo() {
        final var project = upsertProject();
        projectRepo
            .findOwnerOfProjectContainingRepo(project.githubRepo().id())
            .as(StepVerifier::create)
            .expectNext(project.creator())
            .verifyComplete();
    }

    private Project upsertProject() {
        final var creator = userRepo.upsert(lamtevNoIdentity).block();
        assertNotNull(creator);
        final var owner = githubUserRepo.upsert(polisHighload2019.owner()).block();
        assertNotNull(owner);
        final var project = projectRepo.upsert(polisHighload2019, creator).block();
        assertNotNull(project);
        return project;
    }

    private List<GithubRepo> upsertRepos() {
        return Stream
            .of(polisHighload2017, vaddyaHighload2019)
            .map(repo -> {
                final var owner = githubUserRepo.upsert(repo.owner()).block();
                assertNotNull(owner);
                final var upsertedRepo = repoRepo.upsert(repo).block();
                assertNotNull(upsertedRepo);
                return upsertedRepo;
            })
            .toList();
    }

    private List<GithubUser> upsertGithubUsers() {
        return Objects.requireNonNull(githubUserRepo.upsert(TestData.usersGithub).collectList().block());
    }
}
