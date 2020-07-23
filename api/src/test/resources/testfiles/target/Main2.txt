package name;

public class Main {
    @java.lang.Override
    public java.lang.String toString() {

        var abc = 11;

        var xyz = 12;

        Stream.of("abcdefgh", "123cdeabc", "xyz", "6789xy0-")
                .map(Parser::getTokenizedString)
                .forEach(suffixTree::addSequence);

        return "Main{}";
    }
}