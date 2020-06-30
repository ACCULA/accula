package org.accula.analyzer;

//import com.suhininalex.clones.core.CloneIndexer;
//import com.suhininalex.clones.core.structures.Token;

import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;
import org.accula.parser.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Analyzer {
    private static final Integer minCloneLength = 5;

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

        final var tree = new SuffixTree<Token>();
        DataProvider
                .getFiles("testData", "java")
                .map(Parser::getFunctionsAsTokens)
                .forEach(file -> file.forEach(tree::addSequence));

        System.out.println(tree);

        final var terminalNodes = SuffixTreeUtils.dfs(tree.getRoot());
        final var reversedLinks = SuffixTreeUtils.reverseSuffixLink(terminalNodes);
        final var siblings = new HashMap<Clone, List<Clone>>();
        terminalNodes
                .stream()
                .flatMap(SuffixTreeUtils::getClonesFromNode)
                .filter(list -> SuffixTreeUtils.isAllowed(list.get(0).getParent(), reversedLinks))
                .map(SuffixTreeUtils::extractCloneInfo)
                .flatMap(cloneInfo -> {
                    siblings.putIfAbsent(cloneInfo.getReal(), new ArrayList<>());
                    siblings.get(cloneInfo.getReal()).add(cloneInfo.getClone());

                    return siblings
                            .get(cloneInfo.getReal())
                            .stream()
                            .map(clone -> new CloneInfo(cloneInfo.getClone(), clone, cloneInfo.getCloneLength()));
                })
                .filter(Analyzer::filterClones)
                .forEach(System.out::println);
    }

    private static boolean filterClones(CloneInfo cloneInfo) {
        return !Objects.equals(cloneInfo.getClone().getOwner(), cloneInfo.getReal().getOwner())
                && cloneInfo.getCloneLength() > minCloneLength;
    }
}
