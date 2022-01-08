package org.accula.api.code.git.diff;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.accula.api.code.git.CommitDiff;
import org.accula.api.code.git.FileDiff;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Anton Lamtev
 */
public final class CommitDiffParsingIterator implements Iterator<CommitDiff> {
    private final FileDiffParser fileDiffParser = new FileDiffParser();
    private final PeekingIterator<String> diff;
    @Nullable
    private CommitDiff next;

    public CommitDiffParsingIterator(final Iterator<String> diff) {
        this.diff = Iterators.peekingIterator(diff);
    }

    @Override
    public boolean hasNext() {
        tryAdvanceNext();
        return next != null;
    }

    @Override
    public CommitDiff next() {
        tryAdvanceNext();
        try {
            final var next = this.next;
            if (next == null) {
                throw new NoSuchElementException();
            }
            return next;
        } finally {
            next = null;
        }
    }

    private void tryAdvanceNext() {
        if (next != null) {
            return;
        }
        if (!diff.hasNext()) {
            return;
        }

        DiffParsingUtils.consumeNextUntilMatchesPredicate(diff, DiffParsingUtils::isCommitSha);
        if (!diff.hasNext()) {
            return;
        }
        final var line = diff.next();
        final var sha = line.substring(0, 40);
        final var changes = new ArrayList<FileDiff>();
        while (diff.hasNext()) {
            if (DiffParsingUtils.isCommitSha(diff.peek())) {
                break;
            }
            final var change = fileDiffParser.parse(diff);
            if (change == null) {
                break;
            }
            changes.add(change);
        }

        next = new CommitDiff(sha, changes);
    }
}
