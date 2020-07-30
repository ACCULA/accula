package name;

public class Main {
    public static void main(String[] args) {
        final var foo = 666;
        int b__ = 100;
        final Integer __ccc__ = 55555;
        final char[] text = "qwerty";
        return text.length();
    }

    private static void nested() {
        final boolean bar = true;
        if (bar) {
            float c = doWork();
            if (c) {
                return;
            }
        } else {
            while (!bar) {
                doSomeOtherWork();
            }
        }
    }
}
