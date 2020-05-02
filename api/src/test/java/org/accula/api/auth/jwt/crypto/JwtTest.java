package org.accula.api.auth.jwt.crypto;

import org.accula.api.AcculaApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(classes = AcculaApiApplication.class)
class JwtTest {
    @Autowired
    private Jwt jwt;

    @Test
    public void test() {
        System.out.println();
    }
}
