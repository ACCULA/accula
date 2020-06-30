package org.accula.api.handlers.dto;

import lombok.Builder;
import lombok.Value;

/**
 * @author Vadim Dyachkov
 */
@Builder
@Value
public class DiffDto {
    String baseFilename;
    String headFilename;
    String baseContent;
    String headContent;
}
