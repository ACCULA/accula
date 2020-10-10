package org.accula.api.handlers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Vadim Dyachkov
 */
@Builder
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
public class ProjectConfDto implements InputDto {
    List<Long> admins;
    Integer cloneMinTokenCount;
    Integer fileMinSimilarityIndex;
    List<String> excludedFiles;
}
