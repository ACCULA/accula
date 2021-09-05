package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
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
        public static final String KEEP_EXCLUDED_FILES_SYNCED = "Keep list synced with project primary git repo";
        private static final Conf DEFAULT = builder()
                .adminIds(List.of())
                .cloneMinTokenCount(50)
                .fileMinSimilarityIndex(5)
                .excludedFiles(List.of())
                .languages(List.of(CodeLanguage.values()))
                .build();

        List<Long> adminIds;
        Integer cloneMinTokenCount;
        Integer fileMinSimilarityIndex;
        List<String> excludedFiles;
        @Singular
        List<CodeLanguage> languages;
        List<Long> excludedSourceAuthorIds;

        public static Conf defaultConf() {
            return DEFAULT;
        }

        public boolean keepsExcludedFilesSyncedWithGit() {
            return excludedFiles.contains(KEEP_EXCLUDED_FILES_SYNCED);
        }
    }
}
