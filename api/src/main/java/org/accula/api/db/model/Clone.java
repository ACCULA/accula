package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Builder(toBuilder = true)
@With
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Clone {
    @EqualsAndHashCode.Include
    @Builder.Default
    Long id = -1L;
    Snippet target;
    Snippet source;
    @Builder.Default
    Boolean suppressed = Boolean.FALSE;

    @Builder
    @With
    @Value
    public static class Snippet {
        @Builder.Default
        Long id = -1L;
        Snapshot snapshot;
        String file;
        Integer fromLine;
        Integer toLine;
    }
}
