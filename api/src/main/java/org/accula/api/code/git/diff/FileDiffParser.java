package org.accula.api.code.git.diff;

import com.google.common.base.Preconditions;
import com.google.common.collect.PeekingIterator;
import org.accula.api.code.git.FileDiff;
import org.accula.api.code.git.GitFile;
import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.accula.api.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Lamtev
 */
final class FileDiffParser {
    @Nullable
    FileDiff parse(final PeekingIterator<String> lines) {
        final var diffBuilder = new DiffBuilder();
        var state = State.START;
        while ((state = state.process(lines, diffBuilder)) != State.END) ;
        return diffBuilder.build();
    }

    private enum State {
        START {
            @Override
            boolean isReached(final String line) {
                return DiffParsingUtils.isFileStart(line);
            }

            @Override
            boolean isStopWordReached(final String line) {
                return DiffParsingUtils.isFileIndex(line) || DiffParsingUtils.isHunk(line) || DiffParsingUtils.isCommitSha(line);
            }

            @Override
            State processAndGetNextState(final PeekingIterator<String> lines, final DiffBuilder builder) {
                if (!lines.hasNext()) {
                    return State.END;
                }
                final var line = lines.peek();
                final var components = Strings.splitBySpaceIgnoringSpacesBetweenQuotes(line);
                final var componentIdx = DiffParsingUtils.isDiffGit(line) ? 3 : DiffParsingUtils.isDiffCc(line) ? 2 : Integer.MAX_VALUE;
                if (componentIdx >= components.length) {
                    return State.END;
                }
                final var filenameComponent = components[componentIdx];
                if (filenameComponent.length() <= 2) {
                    return State.END;
                }
                final String filename;
                if (filenameComponent.startsWith("\"") && filenameComponent.endsWith("\"")) {
                    filename = filenameComponent.substring(3, filenameComponent.length() - 1);
                } else {
                    filename = filenameComponent.substring(2);
                }
                builder.filename(filename);
                return State.ID;
            }
        },
        ID {
            @Override
            boolean isReached(final String line) {
                return DiffParsingUtils.isFileIndex(line);
            }

            @Override
            boolean isStopWordReached(final String line) {
                return DiffParsingUtils.isHunk(line) || DiffParsingUtils.isCommitSha(line);
            }

            @Override
            State processAndGetNextState(final PeekingIterator<String> lines, final DiffBuilder builder) {
                if (!lines.hasNext()) {
                    return State.END;
                }

                final var line = lines.peek();
                if (DiffParsingUtils.isFileIndex(line)) {
                    final var l = lines.peek();
                    final var components = l.split("\\s+");
                    final var componentCount = components.length;
                    final var isIndex = componentCount > 1 && components[0].equals("index");
                    if (!isIndex) {
                        throw new IllegalStateException();
                    }
                    final var fileIds = components[1];
                    final var fileIdStart = fileIds.indexOf("..") + 2;
                    if (fileIdStart != 1) {
                        builder.id(fileIds.substring(fileIdStart));
                        return State.HUNKS;
                    }
                    throw new IllegalStateException();
                }
                throw new IllegalStateException();
            }
        },
        HUNKS {
            @Override
            boolean isReached(final String line) {
                return DiffParsingUtils.isHunk(line);
            }

            @Override
            boolean isStopWordReached(final String line) {
                return DiffParsingUtils.isCommitSha(line) || DiffParsingUtils.isFileStart(line);
            }

            @Override
            State processAndGetNextState(final PeekingIterator<String> lines, final DiffBuilder builder) {
                // TODO: simplify
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
        },
        END {
            @Override
            boolean isReached(final String line) {
                return false;
            }

            @Override
            boolean isStopWordReached(final String line) {
                return false;
            }

            @Override
            State processAndGetNextState(final PeekingIterator<String> lines, final DiffBuilder builder) {
                throw new IllegalStateException();
            }
        };

        abstract boolean isReached(final String line);

        abstract boolean isStopWordReached(final String line);

        abstract State processAndGetNextState(final PeekingIterator<String> lines, final DiffBuilder builder);

        State process(final PeekingIterator<String> lines, final DiffBuilder builder) {
            return switch (DiffParsingUtils.consumeNextUntilMatchesPredicateOrPredicate(lines, this::isReached, this::isStopWordReached)) {
                case FIRST_MATCH -> processAndGetNextState(lines, builder);
                case SECOND_MATCH, NO_MATCH -> State.END;
            };
        }
    }

    private static final class DiffBuilder {
        final List<LineRange> lineRanges = new ArrayList<>();
        @Nullable
        String id;
        @Nullable
        String filename;

        void id(final String id) {
            Preconditions.checkArgument(this.id == null);
            this.id = id;
        }

        void filename(final String filename) {
            Preconditions.checkArgument(this.filename == null);
            this.filename = filename;
        }

        void lineRange(final LineRange lineRange) {
            lineRanges.add(lineRange);
        }

        @Nullable
        FileDiff build() {
            if (id == null || filename == null) {
                return null;
            }
            return FileDiff.of(GitFile.of(id, filename), LineSet.of(lineRanges));
        }
    }
}
