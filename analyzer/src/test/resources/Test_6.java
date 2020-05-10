package ru.mail.polis.homework.objects;
import java.util.Objects;
// https://github.com/polis-mail-ru/java-tasks/pull/177/files#diff-4e2f7a790275118daa9ecc0874fa5862
/**
 * Нужно найти символ, который встречается подряд в строке чаще всего, и указать количество повторений.
 * Если более одного символа с максимальным значением, то нужно вернуть тот символ,
 * который первый встречается в строчке
 * Если строка пустая или null, то вернуть null
 * Пример abbasbdlbdbfklsssbb -> (s, 3)
 */
public class RepeatingCharacters {

    public static Pair<Character, Integer> getMaxRepeatingCharacters(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        int maxSequence = 0;
        int tmp = 1;
        char symbol = ' ';
        for (int i = 0; i < str.length() - 1; i++) {
            if (str.charAt(i) == str.charAt(i + 1)) {
                tmp += 1;
            } else {
                tmp = 1;
            }
            if (tmp > maxSequence) {
                maxSequence = tmp;
                symbol = str.charAt(i);
            }
        }
        return new Pair<>(symbol, maxSequence);
    }

    public static class Pair<T, V> {
        private final T first;
        private final V second;
        public Pair(T first, V second) {
            this.first = first;
            this.second = second;
        }
        public T getFirst() {
            return first;
        }
        public V getSecond() {
            return second;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
        }
    }
}
