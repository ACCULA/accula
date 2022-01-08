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
    private static final GitFile DEV_NULL = GitFile.of("00000000", "/dev/null");
    String id;
    @EqualsAndHashCode.Exclude
    String name;

    public static GitFile devNull() {
        return DEV_NULL;
    }

    public boolean isDeleted() {
        final int length = Math.min(id.length(), 40);
        for (int i = 0; i < length; ++i) {
            if (id.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    public boolean isDevNull() {
        return this == DEV_NULL || this.equals(DEV_NULL);
    }
}
