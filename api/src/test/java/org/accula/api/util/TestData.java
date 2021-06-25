package org.accula.api.util;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.db.model.User.Role;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Anton Lamtev
 */
public final class TestData {
    private static final AtomicLong id = new AtomicLong();
    public static final GithubUser lamtevGithub = GithubUser.builder()
        .id(10428179L)
        .login("lamtev")
        .name("Anton Lamtev")
        .avatar("https://avatars.githubusercontent.com/u/10428179?v=4")
        .isOrganization(false)
        .build();
    public static final GithubUser vaddyaGithub = GithubUser.builder()
        .id(15687094L)
        .login("vaddya")
        .name("Vadim Dyachkov")
        .avatar("https://avatars.githubusercontent.com/u/15687094?v=4")
        .isOrganization(false)
        .build();
    public static final GithubUser adminGithub = GithubUser.builder()
        .id(12345L)
        .login("some-admin")
        .name("An admin")
        .avatar("avatar")
        .isOrganization(false)
        .build();
    public static final GithubUser admin1Github = GithubUser.builder()
        .id(54321L)
        .login("another-admin")
        .name("An admin")
        .avatar("avatar")
        .isOrganization(false)
        .build();
    public static final GithubUser userGithub = GithubUser.builder()
        .id(987654321L)
        .login("user")
        .avatar("ava")
        .isOrganization(false)
        .build();
    public static final GithubUser user1Github = GithubUser.builder()
        .id(987456321L)
        .login("user1")
        .avatar("ava")
        .isOrganization(false)
        .build();
    public static final GithubUser user2Github = GithubUser.builder()
        .id(789456321L)
        .login("user2")
        .name("")
        .avatar("ava")
        .isOrganization(false)
        .build();
    public static final GithubUser acculaGithub = GithubUser.builder()
        .id(61988411L)
        .login("accula")
        .name("ACCULA")
        .avatar("https://avatars.githubusercontent.com/u/61988411?v=4")
        .isOrganization(true)
        .build();
    public static final GithubUser polisGithub = GithubUser.builder()
        .id(31819365L)
        .login("polis-mail-ru")
        .name("polis-mail-ru")
        .avatar("https://avatars.githubusercontent.com/u/31819365?v=4")
        .isOrganization(true)
        .build();

    public static final User lamtev = new User(id.incrementAndGet(), "ghr_accessToken", lamtevGithub, Role.ROOT);
    public static final User vaddya = new User(id.incrementAndGet(), "ghr_accessToken", vaddyaGithub, Role.ROOT);
    public static final User admin = new User(id.incrementAndGet(), "xxx", adminGithub, Role.ADMIN);
    public static final User admin1 = new User(id.incrementAndGet(), "kkk", admin1Github, Role.ADMIN);
    public static final User user = new User(id.incrementAndGet(), "yyy", userGithub, Role.USER);
    public static final User user1 = new User(id.incrementAndGet(), "zzz", user1Github, Role.USER);
    public static final User user2 = new User(id.incrementAndGet(), "www", user2Github, Role.USER);
    public static final User accula = new User(id.incrementAndGet(), "vvv", acculaGithub, Role.USER);
    public static final User polis = new User(id.incrementAndGet(), "uuu", polisGithub, Role.USER);
    public static final List<User> users = List.of(lamtev, vaddya, admin, admin1, user, user1, user2, accula, polis);

    public static final User lamtevNoIdentity = User.noIdentity(lamtev.githubAccessToken(), lamtev.githubUser(), lamtev.role());

    public static final GithubRepo acculaAccula = GithubRepo.builder()
        .id(246121041L)
        .name("accula")
        .isPrivate(false)
        .description("description")
        .owner(acculaGithub)
        .build();
    public static final GithubRepo polisHighload2019 = GithubRepo.builder()
        .id(211175384L)
        .name("2019-highload-dht")
        .isPrivate(false)
        .description("descr")
        .owner(polisGithub)
        .build();
    public static final GithubRepo vaddyaHighload2019 = GithubRepo.builder()
        .id(211855364L)
        .name("2019-highload-dht")
        .isPrivate(false)
        .description("descr")
        .owner(vaddyaGithub)
        .build();

    public static final Project acculaProject = Project.builder()
        .id(id.incrementAndGet())
        .state(Project.State.CONFIGURING)
        .githubRepo(acculaAccula)
        .creator(lamtev)
        .openPullCount(0)
        .build();

    public static final Pull acculaPull256 = Pull.builder()
        .id(674101692L)
        .number(256)
        .isOpen(true)
        .createdAt(Instant.parse("2021-06-20T21:38:24Z"))
        .updatedAt(Instant.parse("2021-06-29T07:02:17Z"))
        .author(acculaGithub)
        .title("Introduce user roles (#239)")
        .head(Snapshot.builder().repo(acculaAccula).branch("lamtev/#239-add-roles").build())
        .base(Snapshot.builder().repo(acculaAccula).branch("develop").build())
        .primaryProjectId(acculaProject.id())
        .build();
}
