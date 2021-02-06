package org.accula.api.github.model;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubApiSnapshot {
    @Nullable
    String label;
    String ref;
    @Nullable
    GithubApiUser user;
    @Nullable
    GithubApiRepo repo;
    String sha;
}
