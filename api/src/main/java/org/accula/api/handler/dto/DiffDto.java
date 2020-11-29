package org.accula.api.handler.dto;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vadim Dyachkov
 */
@Builder
@Value
public class DiffDto {
    @Nullable
    String baseFilename;
    @Nullable
    String headFilename;
    @Nullable
    String baseContent;
    @Nullable
    String headContent;
}
