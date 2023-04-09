package org.accula.api.db.model;

/**
 * @author Anton Lamtev
 */
public record PullSnapshots(Pull pull, Iterable<Snapshot> snapshots) {
}
