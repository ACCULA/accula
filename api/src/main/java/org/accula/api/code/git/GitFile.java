package org.accula.api.code.git;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PACKAGE)
public class GitFile implements Identifiable {
    private static final GitFile DEV_NULL = GitFile.of("00000000", "/dev/null");
    String id;
    @EqualsAndHashCode.Exclude
    String name;

    static GitFile devNull() {
        return DEV_NULL;
    }

    public boolean isDeleted() {
        for (int i = 0, len = Math.min(id.length(), 40); i < len; ++i) {
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
