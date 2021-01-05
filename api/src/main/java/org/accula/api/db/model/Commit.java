package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class Commit {
    @EqualsAndHashCode.Include
    String sha;
    String authorName;
    String authorEmail;
    Instant date;

    public static Commit shaOnly(final String sha) {
        return new Commit(sha, "", "", Instant.EPOCH);
    }
}
