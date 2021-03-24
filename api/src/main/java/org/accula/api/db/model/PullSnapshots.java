package org.accula.api.db.model;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class PullSnapshots {
    Pull pull;
    Iterable<Snapshot> snapshots;
}
