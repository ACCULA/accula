package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

import java.util.List;

/**
 * @author Anton Lamtev
 */
@Builder
@With
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Project {
    @EqualsAndHashCode.Include
    Long id;
    State state;
    GithubRepo githubRepo;
    @Builder.Default
    List<GithubRepo> secondaryRepos = List.of();
    User creator;
    @Builder.Default
    Integer openPullCount = 0;
    @Builder.Default
    List<Long> adminIds = List.of();

    public enum State {
        CONFIGURING,
        CONFIGURED,
    }

    @Builder
    @With
    @Value
    public static class Conf {
        public static final String KEEP_FILE_LIST_SYNCED_WITH_GIT = "Keep list synced with project primary git repo";
        public static final Conf DEFAULT = builder()
                .adminIds(List.of())
                .cloneMinTokenCount(15)
                .fileMinSimilarityIndex(5)
                .excludedFiles(List.of())
                .build();

        List<Long> adminIds;
        Integer cloneMinTokenCount;
        Integer fileMinSimilarityIndex;
        List<String> excludedFiles;

        public boolean keepsExcludedFilesSyncedWithGit() {
            return excludedFiles.contains(KEEP_FILE_LIST_SYNCED_WITH_GIT);
        }
    }
}
