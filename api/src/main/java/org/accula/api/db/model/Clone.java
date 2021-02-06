package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Builder
@With
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Clone {
    @EqualsAndHashCode.Include
    @Builder.Default
    Long id = -1L;
    Snapshot targetSnapshot;
    String targetFile;
    Integer targetFromLine;
    Integer targetToLine;
    Snapshot sourceSnapshot;
    String sourceFile;
    Integer sourceFromLine;
    Integer sourceToLine;
    @Builder.Default
    Boolean suppressed = Boolean.FALSE;
}
