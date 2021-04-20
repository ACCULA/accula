package org.accula.api.code.git;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author Anton Lamtev
 */
public final class GitNonblockingImpl implements Git {
    @Override
    public CompletableFuture<Repo> repo(final Path directory) {
        return null;
    }

    @Override
    public CompletableFuture<Repo> clone(final String url, final String subdirectory) {
        return null;
    }
}
