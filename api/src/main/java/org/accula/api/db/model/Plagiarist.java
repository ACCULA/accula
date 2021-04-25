package org.accula.api.db.model;

import lombok.Builder;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class Plagiarist {
    GithubUser user;
    Integer cloneCount;
}
