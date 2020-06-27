package org.accula.analyzer;

//import com.suhininalex.clones.core.CloneIndexer;
//import com.suhininalex.clones.core.structures.Token;

import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;
import org.accula.parser.Token;

import java.io.IOException;
import java.util.Objects;

public class Analyzer {
    public static void main(String[] args) throws IOException {
//        SuffixTree<com.suhininalex.clones.core.structures.Token> suffixTree = CloneIndexer.INSTANCE.getTree();

//        DataProvider
//                .getFiles("testData", "java")
//                .forEach(file ->
//                        Parser.getFunctionsAsTokens(file)
//                                .forEach(suffixTree::addSequence));
//
//        final int sequenceId = 3;

//        CloneIndexer.INSTANCE.getTree().getSequence(sequenceId).forEach(token ->
//                System.out.println(token + ":" +
//                        token.getType() + ":" +
//                        Java9Lexer.VOCABULARY.getSymbolicName(token.getType()))
//        );

//        System.out.println(CloneIndexer.INSTANCE.getTree().getSequence(sequenceId));

//        CloneIndexer.INSTANCE.getAllSequenceCloneClasses(sequenceId, 3).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
//                Token end = treeCloneClass.getClones().iterator().next().getLastElement();
//                System.out.print("Begin: " + begin + " | line: " + begin.getLine() + " | file: " + begin.getFilename() + "\t");
//                System.out.println(" || End: " + end + " | line: " + end.getLine() + " | file: " + end.getFilename());
//            }
//        });

//        CloneIndexer.INSTANCE.getAllCloneClasses(3).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
//                Token end = treeCloneClass.getClones().iterator().next().getLastElement();
//                System.out.print("Begin: " + begin + " | line: " + begin.getLine() + " | file: " + begin.getFilename() + "\t");
//                System.out.println(" || End: " + end + " | line: " + end.getLine() + " | file: " + end.getFilename());
//            }
//        });

        //TODO: Clones from Alice/Main.java file not found (BUG!!!);

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
