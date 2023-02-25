package org.accula.api.db.model;

import lombok.Builder;

/**
 * @author Anton Lamtev
 */
@Builder
public record CloneStatistics(GithubUser user, Integer cloneCount, Integer lineCount) {
}
