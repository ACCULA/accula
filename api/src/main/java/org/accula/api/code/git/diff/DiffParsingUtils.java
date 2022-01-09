package org.accula.api.code.git.diff;

import com.google.common.collect.PeekingIterator;
import org.accula.api.util.Strings;

import java.util.function.Predicate;

/**
 * @author Anton Lamtev
 */
final class DiffParsingUtils {
    private DiffParsingUtils() {
    }

    static boolean isFileStart(final String line) {
        if (!line.startsWith("diff --")) {
            return false;
        }
        final var unescaped = line.replace("\"", "");
        return unescaped.startsWith("diff --git a/") ||
               unescaped.startsWith("diff --cc");
    }

    static boolean isFileIndex(final String line) {
        return line.startsWith("index ");
    }

    static boolean isFileRenaming(final String line) {
        return "similarity index 100%".equals(line) || line.startsWith("rename from") || line.startsWith("rename to");
    }

    static boolean isFilename(final String line) {
        return line.startsWith("+++");
    }

    static boolean isBinaryFiles(final String line) {
        return line.startsWith("Binary files");
    }

    static boolean isNoNewlineAtEndOfFile(final String line) {
        return "\\ No newline at end of file".equals(line);
    }

    static boolean isGeneralHunk(final String line) {
        return line.startsWith("@@ ") && line.length() > 6;
    }

    static boolean isThreeWayMergeHunk(final String line) {
        return line.startsWith("@@@ ") && line.length() > 8;
    }

    static boolean isHunk(final String line) {
        return isGeneralHunk(line) || isThreeWayMergeHunk(line);
    }

    static boolean isCommitSha(final String line) {
        return Strings.startsWithSha40(line);
    }

    static void consumeNextUntilMatchesPredicate(final PeekingIterator<String> lines, final Predicate<String> predicate) {
        while (lines.hasNext()) {
            final var line = lines.peek();
            if (predicate.test(line)) {
                break;
            }
            lines.next();
        }
    }

    static ConsumptionResult consumeNextUntilMatchesPredicateOrPredicate(final PeekingIterator<String> lines,
                                                                         final Predicate<String> first,
                                                                         final Predicate<String> second) {
        while (lines.hasNext()) {
            final var line = lines.peek();
            if (first.test(line)) {
                return ConsumptionResult.FIRST_MATCH;
            }
            if (second.test(line)) {
                return ConsumptionResult.SECOND_MATCH;
            }
            lines.next();
        }
        return ConsumptionResult.NO_MATCH;
    }

    enum ConsumptionResult {
        NO_MATCH,
        FIRST_MATCH,
        SECOND_MATCH,
    }
}
