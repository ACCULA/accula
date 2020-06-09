package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Builder(toBuilder = true)
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Clone {
    @EqualsAndHashCode.Include
    @Builder.Default
    Long id = -1L;
    CommitSnapshot targetSnapshot;
    String targetFile;
    Integer targetFromLine;
    Integer targetToLine;
    CommitSnapshot sourceSnapshot;
    String sourceFile;
    Integer sourceFromLine;
    Integer sourceToLine;
    @Builder.Default
    Boolean suppressed = Boolean.FALSE;
}
