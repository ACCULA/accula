package org.accula.api.db.model;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class Project {
    Long id;
    GithubRepo githubRepo;
    User creator;
    User[] admins;
}
