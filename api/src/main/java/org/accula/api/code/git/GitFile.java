package org.accula.api.code.git;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
@RequiredArgsConstructor(staticName = "of")
public class GitFile implements Identifiable {
    String id;
    @EqualsAndHashCode.Exclude
    String name;

    public boolean isDeleted() {
        final int length = Math.min(id.length(), 40);
        for (int i = 0; i < length; ++i) {
            if (id.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}
