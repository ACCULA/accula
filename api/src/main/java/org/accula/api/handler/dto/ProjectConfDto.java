package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Vadim Dyachkov
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
@Value
public class ProjectConfDto implements InputDto {
    List<Long> admins;
    Integer cloneMinTokenCount;
    Integer fileMinSimilarityIndex;
    List<String> excludedFiles;
}
