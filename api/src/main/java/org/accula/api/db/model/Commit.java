package org.accula.api.db.model;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class Commit {
    String sha;
    GithubRepo repo;
}
