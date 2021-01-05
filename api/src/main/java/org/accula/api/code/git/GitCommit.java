package org.accula.api.code.git;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class GitCommit {
    String sha;
    boolean isMerge;
    String authorName;
    String authorEmail;
    Instant date;
}
