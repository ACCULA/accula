package org.accula.analyzer;

import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;
import org.accula.parser.Token;

import java.io.IOException;
import java.util.Objects;

public class Analyzer {
    public static void main(String[] args) throws IOException {
        final var tree = new SuffixTree<Token>();
        DataProvider
                .getFiles("testData", "java")
                .map(Parser::getFunctionsAsTokens)
                .forEach(file -> file.forEach(tree::addSequence));

        System.out.println(tree);

        final var terminalNodes = SuffixTreeUtils.dfs(tree.getRoot());
        final var reversedLinks = SuffixTreeUtils.reverseSuffixLink(terminalNodes);
        terminalNodes
                .stream()
                .flatMap(SuffixTreeUtils::getClonesFromNode)
                .filter(list -> SuffixTreeUtils.isAllowed(list.get(0).getParent(), reversedLinks))
                .map(SuffixTreeUtils::extractCloneInfo)
                .filter(cloneInfo ->
                        !Objects.equals(cloneInfo.getFirstClone().getOwner(), cloneInfo.getOtherClone().getOwner()))
                .forEach(System.out::println);
    }
}
