package org.accula.analyzer;

import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;
import org.accula.parser.Token;

import java.io.IOException;
import java.util.stream.Stream;

public class Analyzer {
    public static void main(String[] args) throws IOException {
        final var suffixTreeStrings = new SuffixTree<Token>();
        Stream.of("banana", "ananas")
                .map(Parser::getTokenizedString)
                .forEach(suffixTreeStrings::addSequence);

        System.out.println(suffixTreeStrings);

        final var suffixTreeJava = new SuffixTree<Token>();
        DataProvider.getFiles("testData", "java")
                .stream()
                .map(Parser::getFunctionsAsTokens)
                .forEach(file -> file.forEach(suffixTreeJava::addSequence));

        System.out.println(suffixTreeJava);
    }
}
