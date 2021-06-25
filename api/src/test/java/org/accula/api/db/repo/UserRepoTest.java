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
import static org.accula.api.util.TestData.admin1Github;
import static org.accula.api.util.TestData.lamtev;
import static org.accula.api.util.TestData.lamtevNoIdentity;
import static org.accula.api.util.TestData.user1Github;
import static org.accula.api.util.TestData.users;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Lamtev
 */
final class UserRepoTest extends BaseRepoTest {
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

        final var user1 = allUsers
            .stream()
            .filter(user -> user.githubUser().equals(user1Github))
            .findFirst()
            .orElseThrow(AssertionError::new);
        final var admin1 = allUsers
            .stream()
            .filter(user -> user.githubUser().equals(admin1Github))
            .findFirst()
            .orElseThrow(AssertionError::new);

        final var newUserIds = allUsers
            .stream()
            .filter(user -> user.is(Role.ADMIN) && !user.equals(admin1) || user.equals(user1))
            .map(User::id)
            .toList();
        final var newAdminIds = allUsers
            .stream()
            .filter(user -> user.is(Role.USER) && !user.equals(user1) || user.equals(admin1))
            .map(User::id)
            .toList();

        // Make all regular users except `user1` admins and all admins except `admin1` regular users
        StepVerifier.create(userRepo.setAdminRole(newAdminIds))
            .expectNextMatches(updatedAllUsers -> {
                updatedAllUsers.forEach(user -> {
                    final var userId = user.id();
                    if (newAdminIds.contains(userId)) {
                        assertEquals(Role.ADMIN, user.role());
                    } else if (newUserIds.contains(userId)) {
                        assertEquals(Role.USER, user.role());
                    } else {
                        assertEquals(Role.ROOT, user.role());
                    }
                });
                return true;
            })
            .expectComplete()
            .verify();

        // Make everyone except root regular user
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
