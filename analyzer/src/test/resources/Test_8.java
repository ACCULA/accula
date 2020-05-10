package ru.mail.polis.homework.analyzer;
// https://github.com/polis-mail-ru/java-tasks/pull/143/files#diff-d8df446216083b9964df28188609996f

public class TooLongFilter implements TextAnalyzer {

    private final long maxLength;


    public TooLongFilter(long maxLen) {
        this.maxLength = maxLen;
    }


    @Override
    public FilterType analyze(String str) {
        if (str.length() > this.maxLength) {
            return FilterType.TOO_LONG;
        }
        return null;
    }

    @Override
    public long getPriority() {
        return FilterType.TOO_LONG.getPriority();
    }


}
