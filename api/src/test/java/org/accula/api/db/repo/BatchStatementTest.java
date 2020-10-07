package org.accula.api.db.repo;

import lombok.SneakyThrows;
import org.accula.api.db.model.GithubUser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchStatementTest {
    @SuppressWarnings("ConstantConditions")
    BatchStatement statement = BatchStatement.of(null, """
            INSERT INTO user_github
            VALUES ($collection)
            ON CONFLICT DO NOTHING
            """);
    List<GithubUser> users = List.of(
            new GithubUser(0L, "login0", "name 0", "avatar", false),
            new GithubUser(1L, "login1", null, "avatar", false),
            new GithubUser(2L, "login2", "Firstname D' Lastnamiano", "avatar", false),
            new GithubUser(3L, "login3", "Just many ' ' ' ' ' ' single quotes", "avatar", false)
    );


    @Test
    void testBind() {
        statement.bind(users, user -> new Object[]{
                user.getId(),
                user.getLogin(),
                user.getName(),
                user.getAvatar(),
                user.isOrganization(),
        });

        @Language("SQL")//
        String expectedSql = """
                INSERT INTO user_github
                VALUES (0,'login0','name 0','avatar',false),\
                (1,'login1',null,'avatar',false),\
                (2,'login2','Firstname D'' Lastnamiano','avatar',false),\
                (3,'login3','Just many '' '' '' '' '' '' single quotes','avatar',false)
                ON CONFLICT DO NOTHING
                """;

        assertEquals(expectedSql, sql(statement));
    }

    @SneakyThrows
    String sql(BatchStatement s) {
        var f = s.getClass().getDeclaredField("boundSqlProducer");
        f.setAccessible(true);
        return ((Supplier<String>) f.get(s)).get();
    }
}
