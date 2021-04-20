package org.accula.api.code.git;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.accula.api.code.git.GitProcess.git;

class GitProcessTest {
    @SneakyThrows
    @Test
    void test() {
        final var git = git("log", "bf0e088773400434e9dc68bfc6bef4eb72261ce0")
            .directory(Path.of("/Users/anton.lamtev/ACCULA/accula"))
            .enableStderr()
            .start();

        final var res = git.stdout()
            .thenComposeAsync(ret -> {
                try (var out = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(ret.bytes(), 0, ret.size()), StandardCharsets.UTF_8)).lines()) {
                    final var lns = out.toList();
                    return CompletableFuture.completedFuture(lns);
                }
            });
        final var ret = res.get();
        System.out.println(ret);
    }
}
