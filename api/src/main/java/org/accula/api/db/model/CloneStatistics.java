package org.accula.api.db.model;

import lombok.Builder;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class CloneStatistics {
    GithubUser user;
    Integer cloneCount;
    Integer lineCount;
}
