package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

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
    State state;
    GithubRepo githubRepo;
    User creator;
    @Builder.Default
    Integer openPullCount = 0;
    @Builder.Default
    List<Long> adminIds = Collections.emptyList();

    public enum State {
        CREATING,
        CREATED,
        ;
    }

    @Builder
    @With
    @Value
    public static class Conf {
        public static final Conf DEFAULT = builder()
                .adminIds(Collections.emptyList())
                .cloneMinTokenCount(15)
                .fileMinSimilarityIndex(5)
                .excludedFiles(Collections.emptyList())
                .build();

        List<Long> adminIds;
        Integer cloneMinTokenCount;
        Integer fileMinSimilarityIndex;
        List<String> excludedFiles;
    }
}
