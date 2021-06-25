package org.accula.api.db.repo;

import lombok.SneakyThrows;
import org.accula.api.db.model.GithubUser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.accula.api.util.TestData.accula69f5528;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anton Lamtev
 */
class BatchStatementTest {
    BatchStatement statement;
    List<GithubUser> users;

    @BeforeEach
    @SuppressWarnings("ConstantConditions")
    void setUp() {
        statement = BatchStatement.of(null, """
            INSERT INTO user_github
            VALUES ($collection)
            ON CONFLICT DO NOTHING
            """);
        users = List.of(
            GithubUser.builder().id(0L).login("login0").name("name 0").avatar("avatar").isOrganization(false).build(),
            GithubUser.builder().id(1L).login("login1").avatar("avatar").isOrganization(false).build(),
            GithubUser.builder().id(2L).login("login2").name("Firstname D' Lastnamiano").avatar("avatar").isOrganization(false).build(),
            GithubUser.builder().id(3L).login("login3").name("Just many ' ' ' ' ' ' single quotes").avatar("avatar").isOrganization(false).build()
        );
    }

    @Test
    void testBind() {
        Function<GithubUser, Object[]> bind = user -> Bindings.of(
            user.id(),
            user.login(),
            user.name(),
            user.avatar(),
            user.isOrganization()
        );
        statement.bind(users, bind);

        @Language("SQL")//
        String expectedSql = """
                (0,'login0','name 0','avatar',false),\
                (1,'login1',null,'avatar',false),\
                (2,'login2','Firstname D'' Lastnamiano','avatar',false),\
                (3,'login3','Just many '' '' '' '' '' '' single quotes','avatar',false)\
                """;

        assertEquals(expectedSql, sql(statement));

        setUp();
        statement.bind(users.stream(), bind);
        assertEquals(expectedSql, sql(statement));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testSequentialBind() {
        final var statement = BatchStatement.of(null, """
            INSERT INTO commit
            VALUES ($collection)
            """);
        IntStream.rangeClosed(0, 1).forEach(__ ->
            statement.bind(List.of(accula69f5528), commit -> Bindings.of(
                commit.sha(),
                commit.isMerge(),
                commit.authorName(),
                commit.authorEmail(),
                commit.date()
            )));

        @Language("SQL")//
        String expectedSql = """
            ('69f552851f0f6093816c3064b6e00438e0ff3b19',false,'Anton Lamtev','antonlamtev@gmail.com','2020-05-03T13:27:09Z'),\
            ('69f552851f0f6093816c3064b6e00438e0ff3b19',false,'Anton Lamtev','antonlamtev@gmail.com','2020-05-03T13:27:09Z')\
            """;

        assertEquals(expectedSql, sql(statement));
    }

    @Test
    void testBindFailure() {
        assertThrows(IllegalArgumentException.class, () -> statement.bind(Stream.empty(), user -> Bindings.of()));
        assertThrows(IllegalArgumentException.class, () -> {
            statement.bind(users, user -> Bindings.of());
            sql(statement);
        });
        @SuppressWarnings("ConstantConditions") final var statement = BatchStatement.of(null, """
            INSERT INTO commit
            VALUES ($collection)
            """);
        statement.bind(users, Bindings::of);
        assertThrows(IllegalArgumentException.class, () -> sql(statement));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testCreationFail() {
        assertThrows(IllegalArgumentException.class, () ->
                BatchStatement.of(null, """
                        INSERT INTO user_ (id, github_id, github_access_token)
                        VALUES (1, 1, 'token')
                        """));
    }

    @Test
    void testExecuteFail() {
        assertThrows(IllegalStateException.class, () -> statement.execute());
        statement.bind(users, user -> Bindings.of(
            user.id(),
            user.login(),
            user.name(),
            user.avatar(),
            user.isOrganization()
        ));
        assertThrows(NullPointerException.class, () -> statement.execute().then().block());
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    String sql(BatchStatement s) {
        var f = s.getClass().getDeclaredField("boundValuesProducer");
        f.setAccessible(true);
        return ((Supplier<StringBuilder>) f.get(s)).get().toString();
    }
}
