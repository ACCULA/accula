package name;

public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            a = b;
            b = c;
        }
        return print();
    }

    private String print() {
        var a = 0;
        var b = 0;
        var c = 0;
        var text = "Hello, clone!";

//        var a = 0;
//        var b = 0;
//        var c = 0;
//        var text = "Hello, clone!";

        return text;
    }

    private void foo() {
        final var l = List.of("void", "f", "print");
        final var l2 = List.of("void", "f", "print", "x");
        final var l3 = List.of("void", "f", "print", "x", "for", "x", "array", "if", "==", "return");
    }

    void f() {
        print();
    }

    void f2() {
        print(x);
    }

    void f3() {
        print(x);
        if (x == array)
            return;
    }
}
