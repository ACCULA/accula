package org.accula.api.code.git;

import lombok.Value;
import org.accula.api.util.Strings;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Iterator that parses entries in the given lines during iteration
 *
 * <p>
 * Lines format:
 * <pre>{@code
 * commit commit_sha
 * Author: author_name <author@email>
 * Date:   date_in_rfc_format
 * }</pre>
 *
 * @author Anton Lamtev
 */
final class CommitEntryParseIterator implements Iterator<CommitEntryParseIterator.Entry> {
    private final List<String> lines;
    private final int lineCount;
    private State state = State.SHA;
    private int i = -1;

    //TODO: process stream/iterator instead of list
    CommitEntryParseIterator(final List<String> lines) {
        this.lines = lines;
        this.lineCount = lines.size();
    }

    @Override
    public boolean hasNext() {
        return i < lineCount;
    }

    @Override
    public Entry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        while (true) {
            if (i == lineCount - 1) {
                i = lineCount;
                return Entry.TERMINAL;
            }
            final var line = lines.get(++i);
            final var next = switch (state) {
                case SHA -> {
                    final var sha = Strings.suffixAfterPrefix(line, "commit ");
                    if (sha == null) {
                        yield null;
                    }
                    state = State.MERGE;
                    yield Entry.of(sha, State.SHA);
                }
                case MERGE -> {
                    state = State.AUTHOR;
                    final var merge = Strings.suffixAfterPrefix(line, "Merge: ");
                    if (merge == null) {
                        --i;
                        yield null;
                    }
                    yield Entry.of(merge, State.MERGE);
                }
                case AUTHOR -> {
                    final var author = Strings.suffixAfterPrefix(line, "Author: ");
                    Objects.requireNonNull(author, "author is null at line: '%s'".formatted(line));
                    state = State.DATE;
                    yield Entry.of(author, State.AUTHOR);
                }
                case DATE -> {
                    final var date = Strings.suffixAfterPrefix(line, "Date:   ");
                    Objects.requireNonNull(date, "date is null at line: '%s'".formatted(line));
                    state = State.SHA;
                    yield Entry.of(date, State.DATE);
                }
                case FINISHED -> throw new NoSuchElementException();
            };
            if (next != null) {
                return next;
            }
        }
    }

    enum State {
        SHA,
        MERGE,
        AUTHOR,
        DATE,

        FINISHED,
    }

    @Value(staticConstructor = "of")
    static class Entry {
        private static final Entry TERMINAL = Entry.of("", State.FINISHED);

        String line;
        State state;
    }
}
