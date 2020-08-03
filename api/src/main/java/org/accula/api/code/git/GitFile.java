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
    String id;
    @EqualsAndHashCode.Exclude
    String name;
}
