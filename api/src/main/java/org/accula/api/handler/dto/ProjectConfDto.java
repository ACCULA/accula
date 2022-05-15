package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
public record ProjectConfDto(ValuesWithSuggestion<Long, UserDto> admins,
                             Code code,
                             Clones clones) implements InputDto {
    public enum Language {
        JAVA,
        KOTLIN,
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    @Builder
    public record Code(Integer fileMinSimilarityIndex,
                       ValuesWithSuggestion<Language, Language> languages) {
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    @Builder
    public record Clones(Integer minTokenCount,
                         ValuesWithSuggestion<String, String> excludedFiles,
                         ValuesWithSuggestion<Long, GithubUserDto> excludedSourceAuthors) {
    }
}
