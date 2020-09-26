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

    @Builder(toBuilder = true)
    @Value
    public static class Conf {
        public static final Conf DEFAULT = builder()
                .adminIds(Collections.emptyList())
                .cloneMinTokenCount(15)
                .excludedFiles(Collections.emptyList())
                .build();

        List<Long> adminIds;
        Integer cloneMinTokenCount;
        List<String> excludedFiles;
    }
}
