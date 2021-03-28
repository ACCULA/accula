package org.accula.api.code.git;

import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.accula.api.util.Iterators;
import org.accula.api.util.Strings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Anton Lamtev
 */
final class FileChangesParseIterator implements Iterator<Object> {
    private final Iterators.NextResettableIterator<String> lines;

    FileChangesParseIterator(final Iterators.NextResettableIterator<String> lines) {
        this.lines = lines;
    }

    @Override
    public boolean hasNext() {
        return lines.hasNext();
    }

    @Override
    public Object next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var expectedLineType = LineType.FILE_START;

        var fileId = (String) null;
        var filename = (String) null;
        final var lineRanges = new ArrayList<LineRange>();

        while (hasNext()) {
            final var possibleSha = lines.next();
            if (Strings.startsWithSha40(possibleSha)) {
                return possibleSha;
            }
            lines.resetNext(possibleSha);
            switch (expectedLineType) {
                case FILE_START -> {
                    final var line = lines.next();
                    if (isFileStart(line)) {
                        expectedLineType = expectedLineType.next();
                    }
                }
                case ID -> {
                    final var line = lines.next();
                    final var components = line.split("\\s+");
                    final var componentCount = components.length;
                    final var isIndex = componentCount > 1 && components[0].equals("index");
                    if (isIndex) {
                        final var fileIds = components[1];
                        final var fileIdStart = fileIds.indexOf("..") + 2;
                        if (fileIdStart != 1) {
                            fileId = fileIds.substring(fileIdStart);
                            expectedLineType = expectedLineType.next();
                        }
                    }
                }
                case FILENAME -> {
                    final var line = lines.next();
                    final var components = line.split("\\s+");
                    final var componentCount = components.length;
                    final var isFilename = componentCount == 2 && components[0].equals("+++");
                    if (isFilename) {
                        final var secondComponent = components[1];
                        if (secondComponent.length() > 2) {
                            filename = secondComponent.substring(2);
                            expectedLineType = expectedLineType.next();
                        }
                    } else if (isBinaryFiles(line)) {
                        if (components.length == 6) {
                            filename = components[4];
                            return GitFileChanges.of(GitFile.of(fileId, filename), LineSet.all());
                        }
                    }
                }
                case LINES_INFO -> {
                    var line = (String) null;

                    while (hasNext()) {
                        line = lines.next();
                        final var isGeneralHunk = line.startsWith("@@ ") && line.length() > 6;
                        final var isThreeWayMergeHunk = line.startsWith("@@@ ") && line.length() > 8;
                        if (!isGeneralHunk && !isThreeWayMergeHunk) {
                            break;
                        }
                        final var lastIndexOfCommercialAts = line.indexOf(" @@");
                        final var linesInfos = line.substring(isGeneralHunk ? 3 : 4, lastIndexOfCommercialAts).split("\\s+");
                        if (linesInfos.length == 2 || linesInfos.length == 3) {
                            final var linesInfo = linesInfos[linesInfos.length - 1];
                            final var rawLinesInfo = Strings.suffixAfterPrefix(linesInfo, "+");
                            if (rawLinesInfo != null) {
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

                                while (hasNext() && diffLinesRead < count) {
                                    final var l = lines.next();
                                    if (!isThreeWayMergeHunk ? l.startsWith("+") : l.length() > 1 && l.charAt(1) == '+') {
                                        if (rangeStart == -1) {
                                            rangeStart = lineIdx;
                                        }
                                    } else if (rangeStart != -1) {
                                        lineRanges.add(LineRange.of(rangeStart, lineIdx - 1));
                                        rangeStart = -1;
                                    }
                                    if (!l.startsWith("-") && !l.startsWith(" -")) {
                                        diffLinesRead++;
                                        lineIdx++;
                                    }
                                }
                                if (rangeStart != -1) {
                                    lineRanges.add(LineRange.of(rangeStart, lineIdx - 1));
                                }
                            }
                        }
                    }
                    if (isFileStart(line) || Strings.startsWithSha40(line)) {
                        lines.resetNext(line);
                        return GitFileChanges.of(GitFile.of(fileId, filename), LineSet.of(lineRanges));
                    }
                }
            }
            if (fileId != null && filename != null && !lineRanges.isEmpty()) {
                return GitFileChanges.of(GitFile.of(fileId, filename), LineSet.of(lineRanges));
            }
        }

        if (fileId != null && filename != null) {
            return GitFileChanges.of(GitFile.of(fileId, filename), LineSet.empty());
        }

        throw new IllegalStateException("Something went wrong");
    }

    private static boolean isFileStart(final String line) {
        return line.startsWith("diff --git a/") || line.startsWith("diff --cc");
    }

    private static boolean isBinaryFiles(final String line) {
        return line.startsWith("Binary files");
    }

    private enum LineType {
        FILE_START,
        ID,
        FILENAME,
        LINES_INFO,
        ;

        public LineType next() {
            return switch (this) {
                case FILE_START -> ID;
                case ID -> FILENAME;
                case FILENAME -> LINES_INFO;
                case LINES_INFO -> FILE_START;
            };
        }
    }
}
