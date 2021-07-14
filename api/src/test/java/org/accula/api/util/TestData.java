package org.accula.api.util;

import org.accula.api.code.git.GitCommit;
import org.accula.api.converter.CodeToModelConverter;
import org.accula.api.db.model.Commit;
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
    public static final List<GithubUser> usersGithub = List.of(lamtevGithub, vaddyaGithub, adminGithub, admin1Github, userGithub, user1Github, user2Github, acculaGithub, polisGithub);

    private static final AtomicLong userId = new AtomicLong();
    public static final User lamtev = new User(userId.incrementAndGet(), "ghr_accessToken", lamtevGithub, Role.ROOT);
    public static final User vaddya = new User(userId.incrementAndGet(), "ghr_accessToken", vaddyaGithub, Role.ROOT);
    public static final User admin = new User(userId.incrementAndGet(), "xxx", adminGithub, Role.ADMIN);
    public static final User admin1 = new User(userId.incrementAndGet(), "kkk", admin1Github, Role.ADMIN);
    public static final User user = new User(userId.incrementAndGet(), "yyy", userGithub, Role.USER);
    public static final User user1 = new User(userId.incrementAndGet(), "zzz", user1Github, Role.USER);
    public static final User user2 = new User(userId.incrementAndGet(), "www", user2Github, Role.USER);
    public static final User accula = new User(userId.incrementAndGet(), "vvv", acculaGithub, Role.USER);
    public static final User polis = new User(userId.incrementAndGet(), "uuu", polisGithub, Role.USER);
    public static final List<User> users = List.of(lamtev, vaddya, admin, admin1, user, user1, user2, accula, polis);

    public static final User lamtevNoIdentity = User.noIdentity(lamtev.githubAccessToken(), lamtev.githubUser(), lamtev.role());
    public static final User vaddyaNoIdentity = User.noIdentity(vaddya.githubAccessToken(), vaddya.githubUser(), vaddya.role());

    public static final GithubRepo acculaAccula = GithubRepo.builder()
        .id(246121041L)
        .name("accula")
        .isPrivate(false)
        .description("description")
        .owner(acculaGithub)
        .build();
    public static final GithubRepo lamtevHighload2017 = GithubRepo.builder()
        .id(104192667L)
        .name("2017-highload-kv")
        .isPrivate(false)
        .description("Курсовой проект 2017 года курса \"Проектирование высоконагруженных систем\"")
        .owner(lamtevGithub)
        .build();
    public static final GithubRepo vaddyaHighload2017 = GithubRepo.builder()
        .id(105875956L)
        .name("2017-highload-kv")
        .isPrivate(false)
        .description("")
        .owner(vaddyaGithub)
        .build();
    public static final GithubRepo polisHighload2017 = GithubRepo.builder()
        .id(103418817L)
        .name("2017-highload-kv")
        .isPrivate(false)
        .description("Курсовой проект 2017 года курса \"Проектирование высоконагруженных систем\"")
        .owner(polisGithub)
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
    public static final List<GithubRepo> repos = List.of(acculaAccula, lamtevHighload2017, vaddyaHighload2017, polisHighload2017, polisHighload2019, vaddyaHighload2019);

    public static final GitCommit accula69f5528Git = GitCommit.builder()
        .sha("69f552851f0f6093816c3064b6e00438e0ff3b19")
        .isMerge(false)
        .authorName("Anton Lamtev")
        .authorEmail("antonlamtev@gmail.com")
        .date(Instant.parse("2020-05-03T13:27:09Z"))
        .build();
    public static final GitCommit accula485b362Git = GitCommit.builder()
        .sha("485b362c088e93915e50aeb1dfac3f473fee6a6c")
        .isMerge(false)
        .authorName("Anton Lamtev")
        .authorEmail("lamtev@users.noreply.github.com")
        .date(Instant.parse("2021-06-25T06:07:36Z"))
        .build();
    public static final GitCommit accula14dd20fGit = GitCommit.builder()
        .sha("14dd20fe8a501713be8d4eec3f7e2cdc273905a0")
        .isMerge(false)
        .authorName("Anton Lamtev")
        .authorEmail("lamtev@users.noreply.github.com")
        .date(Instant.parse("2021-06-20T10:49:21Z"))
        .build();
    public static GitCommit vaddyaHighload2019_7d8e10cGit = GitCommit.builder()
        .sha("7d8e10c9b91174aacc244590a7d81c23cbb01e8b")
        .isMerge(false)
        .authorName("vaddya")
        .authorEmail("vadik.dyachkov@gmail.com")
        .date(Instant.parse("2019-11-20T06:55:44Z"))
        .build();
    public static final GitCommit highload2019_720cefbGit = GitCommit.builder()
        .sha("720cefb3f361895e9e23524c2b4025f9a949d5d2")
        .isMerge(false)
        .authorName("Vadim TSesko")
        .authorEmail("incubos@users.noreply.github.com")
        .date(Instant.parse("2019-11-22T15:50:32Z"))
        .build();
    public static final GitCommit lamtevHighload17_8ad07b9Git = GitCommit.builder()
        .sha("8ad07b914c0c2cee8b5a47993061b79c611db65d")
        .isMerge(false)
        .authorName("Anton Lamtev")
        .authorEmail("antonlamtev@gmail.com")
        .date(Instant.parse("2018-01-09T15:26:20Z"))
        .build();
    public static final GitCommit highload17_b2b9e4aGit = GitCommit.builder()
        .sha("b2b9e4a1c69cee84bdcb61005b74868fc276a99a")
        .isMerge(false)
        .authorName("Vadim Tsesko")
        .authorEmail("incubos@yandex.com")
        .date(Instant.parse("2017-11-06T18:47:26Z"))
        .build();

    public static final Commit accula69f5528 = CodeToModelConverter.convert(accula69f5528Git);
    public static final Commit accula485b362 = CodeToModelConverter.convert(accula485b362Git);
    public static final Commit accula14dd20f = CodeToModelConverter.convert(accula14dd20fGit);
    public static final Commit vaddyaHighload2019_7d8e10c = CodeToModelConverter.convert(vaddyaHighload2019_7d8e10cGit);
    public static final Commit highload2019_720cefb = CodeToModelConverter.convert(highload2019_720cefbGit);
    public static final Commit lamtevHighload17_8ad07b9 = CodeToModelConverter.convert(lamtevHighload17_8ad07b9Git);
    public static final Commit highload17_b2b9e4a = CodeToModelConverter.convert(highload17_b2b9e4aGit);

    private static final AtomicLong projectId = new AtomicLong();
    public static final Project highload19Project = Project.builder()
        .id(projectId.incrementAndGet())
        .state(Project.State.CONFIGURING)
        .githubRepo(polisHighload2019)
        .creator(lamtev)
        .openPullCount(0)
        .build();
    public static final Project highload17Project = Project.builder()
        .id(projectId.incrementAndGet())
        .state(Project.State.CONFIGURED)
        .githubRepo(polisHighload2017)
        .creator(lamtev)
        .openPullCount(0)
        .build();
    public static final List<Project> projects = List.of(highload19Project, highload17Project);

    public static final Snapshot accula485b362Snap = Snapshot.builder()
        .commit(accula485b362)
        .branch("develop")
        .repo(acculaAccula)
        .build();
    public static final Snapshot accula14dd20fSnap = Snapshot.builder()
        .commit(accula14dd20f)
        .branch("develop")
        .repo(acculaAccula)
        .build();
    public static final Snapshot highload2019_174Head = Snapshot.builder()
        .commit(vaddyaHighload2019_7d8e10c)
        .branch("master")
        .repo(vaddyaHighload2019)
        .build();
    public static final Snapshot highload2019_174Base = Snapshot.builder()
        .commit(highload2019_720cefb)
        .branch("master")
        .repo(polisHighload2019)
        .build();
    public static final Snapshot highload17_61Head = Snapshot.builder()
        .commit(lamtevHighload17_8ad07b9)
        .branch("master")
        .repo(lamtevHighload2017)
        .build();
    public static final Snapshot highload17_61Base = Snapshot.builder()
        .commit(highload17_b2b9e4a)
        .branch("master")
        .repo(polisHighload2017)
        .build();

    public static final List<Snapshot> snapshots = List.of(highload2019_174Head, accula485b362Snap, highload2019_174Base, accula14dd20fSnap, highload17_61Head, highload17_61Base);

    public static final Pull highload19_174 = Pull.builder()
        .id(342695057L)
        .number(174)
        .isOpen(false)
        .createdAt(Instant.parse("2019-11-19T14:00:41Z"))
        .updatedAt(Instant.parse("2019-11-23T14:54:34Z"))
        .author(vaddyaGithub)
        .title("Leveled Compaction | Vadim Dyachkov")
        .head(highload2019_174Head)
        .base(highload2019_174Base)
        .primaryProjectId(highload19Project.id())
        .build();
    public static final Pull highload17_61 = Pull.builder()
        .id(161901269L)
        .number(61)
        .isOpen(false)
        .createdAt(Instant.parse("2018-01-09T15:30:27Z"))
        .updatedAt(Instant.parse("2018-01-09T16:47:11Z"))
        .author(lamtevGithub)
        .title("hw 3")
        .head(highload17_61Head)
        .base(highload17_61Base)
        .primaryProjectId(highload17Project.id())
        .build();
    public static final List<Pull> pulls = List.of(highload19_174, highload17_61);
}
