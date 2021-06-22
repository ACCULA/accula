package org.accula.api.db.repo;

import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import org.accula.api.db.model.User.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.function.Predicate.not;
import static org.accula.api.util.TestData.lamtev;
import static org.accula.api.util.TestData.lamtevNoIdentity;
import static org.accula.api.util.TestData.users;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
class UserRepoTest extends BaseRepoTest {
    private UserRepo userRepo;

    @BeforeEach
    void setUp() {
        super.setUp();
        userRepo = new UserRepoImpl(connectionProvider());
    }

    @Test
    void testUpsert() {
        StepVerifier.create(userRepo.upsert(lamtevNoIdentity))
            .expectNext(lamtev)
            .expectComplete()
            .verify();
    }

    @Test
    void testFindById() {
        expectCompleteEmpty(userRepo.findById(lamtev.id()));

        StepVerifier.create(userRepo.upsert(lamtev)
                .then(userRepo.findById(lamtev.id())))
            .expectNext(lamtev)
            .expectComplete()
            .verify();
    }

    @Test
    void testFindByGithubIds() {
        expectCompleteEmpty(userRepo.findByGithubIds(List.of()));

        var githubIds = users.stream().map(User::githubUser).map(GithubUser::id).toList();
        StepVerifier.create(userRepo.findByGithubIds(githubIds))
            .expectComplete()
            .verify();

        final var insertedUsers = Flux.fromIterable(users)
            .flatMap(userRepo::upsert)
            .collectList()
            .block();
        assertNotNull(insertedUsers);

        githubIds = insertedUsers.stream().map(User::githubUser).map(GithubUser::id).toList();

        final var usersArray = insertedUsers.toArray(new User[0]);
        StepVerifier.create(userRepo.findByGithubIds(githubIds))
            .expectNext(usersArray)
            .expectComplete()
            .verify();
    }

    @Test
    void testFindAll() {
        expectCompleteEmpty(userRepo.findAll());

        StepVerifier.create(Flux.fromIterable(users)
                .flatMap(userRepo::upsert)
                .thenMany(userRepo.findAll())
                .collectList())
            .expectNextMatches(allUsers -> Set.copyOf(allUsers).containsAll(users))
            .expectComplete()
            .verify();
    }

    @Test
    void testSetAdminRole() {
        expectCompleteEmpty(userRepo.setAdminRole(Set.of(1L, 2L)));

        final var allUsers = Flux.fromIterable(users)
            .flatMap(userRepo::upsert)
            .collectList()
            .block();
        assertNotNull(allUsers);

        final var users = allUsers.stream().filter(Role.USER::is).toList();
        final var userIds = users.stream().map(User::id).toList();
        final var adminIds = allUsers.stream().filter(Role.ADMIN::is).map(User::id).toList();

        // Make regular users admins and vice-versa
        StepVerifier.create(userRepo.setAdminRole(userIds))
            .expectNextMatches(updatedAllUsers -> {
                updatedAllUsers.forEach(user -> {
                    if (userIds.contains(user.id())) {
                        assertEquals(Role.ADMIN, user.role());
                    } else if (adminIds.contains(user.id())) {
                        assertEquals(Role.USER, user.role());
                    } else {
                        assertEquals(Role.ROOT, user.role());
                    }
                });
                return true;
            })
            .expectComplete()
            .verify();

        // Make everyone except roots a regular user
        StepVerifier.create(userRepo.setAdminRole(Set.of()))
            .expectNextMatches(updatedAllUsers -> updatedAllUsers.stream().allMatch(not(Role.ADMIN::is)))
            .expectComplete()
            .verify();
    }

    @Test
    void testAddOnUpsert() {
        final var didUpsert = new AtomicBoolean(false);
        userRepo.addOnUpsert(upsertedUserId -> didUpsert.set(true));

        StepVerifier.create(userRepo.upsert(lamtevNoIdentity))
            .expectNext(lamtev)
            .expectComplete()
            .verify();

        assertTrue(didUpsert.get());
    }
}
