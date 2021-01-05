package org.accula.api.code.git;

import lombok.Value;
import org.accula.api.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
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
    private final Iterator<String> lines;
    private Type type = Type.SHA;
    @Nullable
    private String notYetProcessedNext = null;

    CommitEntryParseIterator(final Iterator<String> lines) {
        this.lines = lines;
    }

    @Override
    public boolean hasNext() {
        return notYetProcessedNext != null || lines.hasNext();
    }

    @Override
    public Entry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        while (true) {
            if (!hasNext()) {
                return Entry.TERMINAL;
            }
            final var line = notYetProcessedNext != null ? notYetProcessedNext : lines.next();
            notYetProcessedNext = null;
            final var next = switch (type) {
                case SHA -> {
                    final var sha = Strings.suffixAfterPrefix(line, "commit ");
                    if (sha == null) {
                        yield null;
                    }
                    type = Type.MERGE;
                    yield Entry.of(sha, Type.SHA);
                }
                case MERGE -> {
                    type = Type.AUTHOR;
                    final var merge = Strings.suffixAfterPrefix(line, "Merge: ");
                    if (merge == null) {
                        notYetProcessedNext = line;
                        yield null;
                    }
                    yield Entry.of(merge, Type.MERGE);
                }
                case AUTHOR -> {
                    final var author = Strings.suffixAfterPrefix(line, "Author: ");
                    Objects.requireNonNull(author, () -> "author is null at line: '%s'".formatted(line));
                    type = Type.DATE;
                    yield Entry.of(author, Type.AUTHOR);
                }
                case DATE -> {
                    final var date = Strings.suffixAfterPrefix(line, "Date:   ");
                    Objects.requireNonNull(date, () -> "date is null at line: '%s'".formatted(line));
                    type = Type.SHA;
                    yield Entry.of(date, Type.DATE);
                }
                case FINISHED -> throw new IllegalStateException();
            };
            if (next != null) {
                return next;
            }
        }
    }

    enum Type {
        SHA,
        MERGE,
        AUTHOR,
        DATE,

        FINISHED,
    }

    @Value(staticConstructor = "of")
    static class Entry {
        private static final Entry TERMINAL = Entry.of("", Type.FINISHED);

        String line;
        Type type;
    }
}
