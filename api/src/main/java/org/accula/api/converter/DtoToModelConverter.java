package org.accula.api.converter;

import org.accula.api.db.model.CodeLanguage;
import org.accula.api.db.model.Project;
import org.accula.api.handler.dto.ProjectConfDto;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public final class DtoToModelConverter {
    private DtoToModelConverter() {
    }

    public static Project.Conf convert(final ProjectConfDto conf) {
        return Project.Conf.builder()
                .adminIds(conf.admins())
                .cloneMinTokenCount(conf.cloneMinTokenCount())
                .fileMinSimilarityIndex(conf.fileMinSimilarityIndex())
                .excludedFiles(conf.excludedFiles())
                .languages(convertLanguages(conf.languages()))
                .build();
    }

    public static List<CodeLanguage> convertLanguages(final Collection<ProjectConfDto.Language> languages) {
        return languages
            .stream()
            .map(DtoToModelConverter::convert)
            .toList();
    }

    public static CodeLanguage convert(final ProjectConfDto.Language language) {
        return switch (language) {
            case JAVA -> CodeLanguage.JAVA;
            case KOTLIN -> CodeLanguage.KOTLIN;
        };
    }
}
