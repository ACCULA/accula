package org.accula.api.converter;

import org.accula.api.db.model.Project;
import org.accula.api.handler.dto.ProjectConfDto;

/**
 * @author Anton Lamtev
 */
public final class DtoToModelConverter {
    private DtoToModelConverter() {
    }

    public static Project.Conf convert(final ProjectConfDto conf) {
        return Project.Conf.builder()
                .adminIds(conf.getAdmins())
                .cloneMinTokenCount(conf.getCloneMinTokenCount())
                .fileMinSimilarityIndex(conf.getFileMinSimilarityIndex())
                .excludedFiles(conf.getExcludedFiles())
                .build();
    }
}
