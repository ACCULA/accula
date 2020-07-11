package org.accula.github.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor
public class GithubApiCommitSnapshot {
    @Nullable
    String label;
    String ref;
    @Nullable
    GithubApiUser user;
    @Nullable
    GithubApiRepo repo;
    String sha;
}
