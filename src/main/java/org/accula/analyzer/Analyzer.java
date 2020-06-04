package org.accula.analyzer;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;

import java.util.stream.Stream;

public class Analyzer {
    public static void main(String[] args) {
        SuffixTree<com.suhininalex.clones.core.structures.Token> suffixTree = CloneIndexer.INSTANCE.getTree();
        Stream.of("abcdefgh", "123cde45abc")
                .map(Parser::getTokenizedString)
                .forEach(suffixTree::addSequence);
        CloneIndexer.INSTANCE.getAllCloneClasses(2).forEach(treeCloneClass -> {
            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
                System.out.println(treeCloneClass.getClones().iterator().next().getFirstElement() + " " +
                                   treeCloneClass.getClones().iterator().next().getLastElement());
            }
        });
    }
}
