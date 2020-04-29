package ru.mail.polis.homework.analyzer.filters;
// https://github.com/polis-mail-ru/java-tasks/pull/145/files#diff-106ef464356da475959a9db2c1c6a2e6

import ru.mail.polis.homework.analyzer.FilterType;
import ru.mail.polis.homework.analyzer.TextAnalyzer;

public class TooLongFilter implements TextAnalyzer {
    private final long maxLength;

    public TooLongFilter(long maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public FilterType analyze(String str) {
        if (str.length() > maxLength) {
            return FilterType.TOO_LONG;
        }
        return null;
    }

    @Override
    public FilterType getType() {
        return FilterType.TOO_LONG;
    }
}
