package org.accula.api.code.git.diff;

import com.google.common.base.Preconditions;
import com.google.common.collect.PeekingIterator;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.git.FileDiff;
import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.accula.api.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Anton Lamtev
 */
final class FileDiffParser {
    @Nullable
    FileDiff parse(final PeekingIterator<String> lines) {
        final var diffBuilder = new DiffBuilder();
        var state = State.START;
        while ((state = switch (state) {
            case START -> processStart(lines);
            case ID -> processId(lines, diffBuilder);
            case NAME -> processName(lines, diffBuilder);
            case CHANGES -> processChanges(lines, diffBuilder);
            case END -> throw new IllegalStateException();
        }) != State.END);
        return diffBuilder.build();
    }

    private static State processStart(final PeekingIterator<String> lines) {
        DiffParsingUtils.consumeNextUntilMatchesPredicate(lines, DiffParsingUtils::isFileStart);
        return State.ID;
    }

    private static State processId(final PeekingIterator<String> lines, final DiffBuilder builder) {
        consumeNextUntilIdReached(lines);

        if (!lines.hasNext()) {
            return State.END;
        }

        final var line = lines.peek();
        if (DiffParsingUtils.isFileIndex(line)) {
            return processIndex(lines, builder);
        } else if (DiffParsingUtils.isFileRenaming(line)) {
            return processRenaming(lines, builder);
        }
        throw new IllegalStateException();
    }

    private static State processIndex(final PeekingIterator<String> lines, final DiffBuilder builder) {
        consumeNextUntilIndexReached(lines);
        final var line = lines.peek();
        final var components = line.split("\\s+");
        final var componentCount = components.length;
        final var isIndex = componentCount > 1 && components[0].equals("index");
        if (!isIndex) {
            throw new IllegalStateException();
        }
        final var fileIds = components[1];
        final var fileIdStart = fileIds.indexOf("..") + 2;
        if (fileIdStart != 1) {
            builder.id(fileIds.substring(fileIdStart));
            return State.NAME;
        }
        throw new IllegalStateException();
    }

    private static State processRenaming(final PeekingIterator<String> lines, final DiffBuilder builder) {
        lines.next();
        if (!lines.hasNext()) {
            return State.END;
        }
        while (lines.hasNext() && DiffParsingUtils.isFileRenaming(lines.peek())) {
            lines.next();
        }
        if (!lines.hasNext()) {
            return State.END;
        }
        final var line = lines.peek();
        if (DiffParsingUtils.isFileIndex(line)) {
            return processIndex(lines, builder);
        }
        return State.END;
    }

    private static State processName(final PeekingIterator<String> lines, final DiffBuilder builder) {
        consumeNextUntilNameReached(lines);

        final var line = lines.peek();
        final var components = line.split("\\s+");
        final var componentCount = components.length;
        final var isFilename = componentCount == 2 && components[0].equals("+++");
        if (isFilename) {
            final var secondComponent = components[1];
            if (secondComponent.length() > 2) {
                builder.filename(secondComponent.substring(2));
                return State.CHANGES;
            }
        } else if (DiffParsingUtils.isBinaryFiles(line)) {
            if (components.length == 6) {
                builder.binary(components[4]);
                return State.END;
            }
        }
        throw new IllegalStateException();
    }

    // TODO: simplify
    private static State processChanges(final PeekingIterator<String> lines, final DiffBuilder builder) {
        consumeNextUntilChangesReached(lines);

        while (lines.hasNext()) {
            final var line = lines.peek();
            if (DiffParsingUtils.isCommitSha(line)) {
                return State.END;
            }
            final var isGeneralHunk = DiffParsingUtils.isGeneralHunk(line);
            final var isThreeWayMergeHunk = DiffParsingUtils.isThreeWayMergeHunk(line);
            if (!isGeneralHunk && !isThreeWayMergeHunk) {
                break;
            }

            lines.next();
            final var lastIndexOfCommercialAts = line.indexOf(" @@");
            final var linesInfos = line.substring(isGeneralHunk ? 3 : 4, lastIndexOfCommercialAts).split("\\s+");
            if (linesInfos.length != 2 && linesInfos.length != 3) {
                return State.END;
            }
            final var linesInfo = linesInfos[linesInfos.length - 1];
            final var rawLinesInfo = Strings.suffixAfterPrefix(linesInfo, "+");
            if (rawLinesInfo == null) {
                return State.END;
            }
            final int start;
            final int count;
            if (rawLinesInfo.equals("1")) {
                start = count = 1;
            } else {
                final var commaPosition = rawLinesInfo.indexOf(',');
                start = Integer.parseInt(rawLinesInfo.substring(0, commaPosition));
                count = Integer.parseInt(rawLinesInfo.substring(commaPosition + 1));
            }
            var diffLinesRead = 0;
            var lineIdx = start;

            var rangeStart = -1;

            while (lines.hasNext() && diffLinesRead < count) {
                final var l = lines.peek();
                if (DiffParsingUtils.isNoNewlineAtEndOfFile(l)) {
                    lines.next();
                    continue;
                }
                if (!isThreeWayMergeHunk ? l.startsWith("+") : l.length() > 1 && l.charAt(1) == '+') {
                    if (rangeStart == -1) {
                        rangeStart = lineIdx;
                    }
                } else if (rangeStart != -1) {
                    builder.lineRange(LineRange.of(rangeStart, lineIdx - 1));
                    rangeStart = -1;
                }
                if (!l.startsWith("-") && (!isThreeWayMergeHunk || !l.startsWith(" -"))) {
                    diffLinesRead++;
                    lineIdx++;
                }
                lines.next();
            }
            if (rangeStart != -1) {
                builder.lineRange(LineRange.of(rangeStart, lineIdx - 1));
            }
        }

        return State.END;
    }

    private static void consumeNextUntilIndexReached(final PeekingIterator<String> lines) {
        DiffParsingUtils.consumeNextUntilMatchesPredicate(
            lines,
            DiffParsingUtils::isFileIndex
        );
    }

    private static void consumeNextUntilIdReached(final PeekingIterator<String> lines) {
        DiffParsingUtils.consumeNextUntilMatchesPredicate(
            lines,
            ((Predicate<String>) DiffParsingUtils::isFileIndex)
                .or(DiffParsingUtils::isFileRenaming)
        );
    }

    private static void consumeNextUntilNameReached(final PeekingIterator<String> lines) {
        DiffParsingUtils.consumeNextUntilMatchesPredicate(lines, ((Predicate<String>) DiffParsingUtils::isFilename).or(DiffParsingUtils::isBinaryFiles));
    }

    private static void consumeNextUntilChangesReached(final PeekingIterator<String> lines) {
        DiffParsingUtils.consumeNextUntilMatchesPredicate(lines, ((Predicate<String>) DiffParsingUtils::isGeneralHunk).or(DiffParsingUtils::isThreeWayMergeHunk));
    }

    private enum State {
        START,
        ID,
        NAME,
        CHANGES,
        END
    }

    private static final class DiffBuilder {
        final List<LineRange> lineRanges = new ArrayList<>();
        @Nullable
        String id;
        @Nullable
        String filename;
        boolean isBinary;

        void id(final String id) {
            Preconditions.checkArgument(this.id == null);
            this.id = id;
        }

        void filename(final String filename) {
            Preconditions.checkArgument(this.filename == null);
            this.filename = filename;
        }

        void binary(final String filename) {
            Preconditions.checkArgument(this.filename == null);
            this.filename = filename;
            this.isBinary = true;
        }

        void lineRange(final LineRange lineRange) {
            lineRanges.add(lineRange);
        }

        @Nullable
        FileDiff build() {
            if (id == null || filename == null) {
                return null;
            }
            return FileDiff.of(GitFile.of(id, filename), isBinary ? LineSet.all() : LineSet.of(lineRanges));
        }
    }
}
