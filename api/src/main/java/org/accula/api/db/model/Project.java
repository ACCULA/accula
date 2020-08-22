package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Project {
    @EqualsAndHashCode.Include
    Long id;
    GithubRepo githubRepo;
    User creator;
    @Builder.Default
    Integer openPullCount = 0;
    @Builder.Default
    List<Long> adminIds = Collections.emptyList();

    @Builder
    @Value
    public static class Conf {
        public static final Conf DEFAULT = builder()
                .adminIds(Collections.emptyList())
                .cloneMinLineCount(5)
                .build();

        List<Long> adminIds;
        Integer cloneMinLineCount;
    }
}
