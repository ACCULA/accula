package org.accula.api.clone;

import lombok.Builder;
import lombok.Value;
import org.accula.api.code.lines.LineRange;
import org.accula.api.db.model.Snapshot;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Builder
@Value
public class CodeClone {
    Snippet source;
    Snippet target;

    @Builder
    @Value
    public static class Snippet {
        Snapshot snapshot;
        String file;
        LineRange lines;
        String method;

        @Override
        public String toString() {
            return snapshot + ":" + file + lines + ":" + method;
        }
    }
}
