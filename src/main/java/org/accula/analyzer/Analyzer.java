package org.accula.analyzer;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.File;
import org.accula.parser.Parser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Analyzer {
    public static void main(String[] args) throws IOException {
        SuffixTree<com.suhininalex.clones.core.structures.Token> suffixTree = CloneIndexer.INSTANCE.getTree();
//        Stream.of("abcdefgh", "123cdeabc", "xyz", "6789xy0-")
//                .map(Parser::getTokenizedString)
//                .forEach(suffixTree::addSequence);
//        CloneIndexer.INSTANCE.getAllCloneClasses(0).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                System.out.println(treeCloneClass.getClones().iterator().next().getFirstElement() + " " +
//                                   treeCloneClass.getClones().iterator().next().getLastElement());
//            }
//        });
//        System.out.println("+++++++++++++++++++++++++++++++++++");
//        CloneIndexer.INSTANCE.getAllSequenceCloneClasses(2, 0).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                System.out.println(treeCloneClass.getClones().iterator().next().getFirstElement() + " " +
//                        treeCloneClass.getClones().iterator().next().getLastElement());
//            }
//        });
//        System.out.println("+++++++++++++++++++++++++++++++++++");
//        CloneIndexer.INSTANCE.getAllSequenceCloneClasses(3, 0).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                System.out.println(treeCloneClass.getClones().iterator().next().getFirstElement() + " " +
//                        treeCloneClass.getClones().iterator().next().getLastElement());
//            }
//        });
//        System.out.println("+++++++++++++++++++++++++++++++++++");
//        CloneIndexer.INSTANCE.getAllSequenceCloneClasses(4, 0).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                System.out.println(treeCloneClass.getClones().iterator().next().getFirstElement() + " " +
//                        treeCloneClass.getClones().iterator().next().getLastElement());
//            }
//        });

        DataProvider
                .getFiles("testData", "java")
                .forEach(file ->
                        Parser.getFunctionsAsTokens(file)
                                .forEach(suffixTree::addSequence));

        CloneIndexer.INSTANCE.getAllSequenceCloneClasses(3, 3).forEach(treeCloneClass -> {
            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
                Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
                Token end = treeCloneClass.getClones().iterator().next().getLastElement();
                System.out.print("Begin: " + begin + "line: " + begin.getLine() + "file: " + begin.getFilename() + "\t");
                System.out.println("End: " + end + "line: " + end.getLine() + "file: " + end.getFilename());
            }
        });
    }
}
