package org.accula.api.code.git;

import java.util.Iterator;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public record CommitDiff(String sha, List<FileDiff> changes) implements Iterable<FileDiff> {
    @Override
    public Iterator<FileDiff> iterator() {
        return changes.iterator();
    }
}
