package org.accula.api.db.repo;

import lombok.SneakyThrows;
import org.accula.api.db.model.GithubUser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchStatementTest {
    @SuppressWarnings("ConstantConditions")
    BatchStatement statement = BatchStatement.of(null, """
            INSERT INTO user_github
            VALUES ($collection)
            ON CONFLICT DO NOTHING
            """);
    List<GithubUser> users = List.of(
            GithubUser.builder().id(0L).login("login0").name("name 0").avatar("avatar").isOrganization(false).build(),
            GithubUser.builder().id(1L).login("login1").avatar("avatar").isOrganization(false).build(),
            GithubUser.builder().id(2L).login("login2").name("Firstname D' Lastnamiano").avatar("avatar").isOrganization(false).build(),
            GithubUser.builder().id(3L).login("login3").name("Just many ' ' ' ' ' ' single quotes").avatar("avatar").isOrganization(false).build()
    );

    @Test
    void testBind() {
        statement.bind(users, user -> Bindings.of(
                user.id(),
                user.login(),
                user.name(),
                user.avatar(),
                user.isOrganization()
        ));

        @Language("SQL")//
        String expectedSql = """
                (0,'login0','name 0','avatar',false),\
                (1,'login1',null,'avatar',false),\
                (2,'login2','Firstname D'' Lastnamiano','avatar',false),\
                (3,'login3','Just many '' '' '' '' '' '' single quotes','avatar',false)\
                """;

        assertEquals(expectedSql, sql(statement));
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

    @SneakyThrows
    String sql(BatchStatement s) {
        var f = s.getClass().getDeclaredField("boundValuesProducer");
        f.setAccessible(true);
        return ((Supplier<StringBuilder>) f.get(s)).get().toString();
    }
}
