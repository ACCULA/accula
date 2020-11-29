package org.accula.api.converter;

import org.accula.api.db.model.Project;
import org.accula.api.handler.dto.InputDto;
import org.accula.api.handler.dto.ProjectConfDto;

import java.io.Serial;

/**
 * @author Anton Lamtev
 */
public final class DtoToModelConverter {
    private DtoToModelConverter() {
    }

    public static Project.Conf convert(final ProjectConfDto conf) {
        validate(conf);
        return Project.Conf.builder()
                .adminIds(conf.getAdmins())
                .cloneMinTokenCount(conf.getCloneMinTokenCount())
                .fileMinSimilarityIndex(conf.getFileMinSimilarityIndex())
                .excludedFiles(conf.getExcludedFiles())
                .build();
    }

    private static void validate(final InputDto dto) {
        if (!dto.isValid()) {
            throw new ValidationException();
        }
    }

    public static class ValidationException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = -885047004998370035L;
    }
}
