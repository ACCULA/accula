package org.accula.core.checkers;

import org.accula.core.checkers.structures.CloneInfo;
import org.accula.core.checkers.structures.Interval;
import org.accula.parser.Java9Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class CloneCheckerImpl implements CloneChecker <ParseTree, CloneInfo> {
    private static final int CLONE_LENGTH = 40;

    @NotNull
    @Override
    public CloneInfo checkClones(@NotNull final ParseTree a,
                                 @NotNull final ParseTree b,
                                 final float threshold) {
        return compareTrees(a, b, threshold, new CloneInfo(0.0f, 0, new HashSet<>(), new HashSet<>()));
    }

    @NotNull
    private CloneInfo compareTrees(@NotNull final ParseTree tree1,
                                   @NotNull final ParseTree tree2,
                                   final float threshold,
                                   @NotNull CloneInfo cloneInfo) {
        if (tree1.getClass() == tree2.getClass() &&
                tree1 instanceof Java9Parser.BlockStatementsContext) {
            if (tree1.getText().length() > CLONE_LENGTH &&
                    tree2.getText().length() > CLONE_LENGTH) {
                var j = new JaccardSimilarity();
                var cmp = j.apply(tree1.getText(), tree2.getText()).floatValue();
                cloneInfo.setCount(cloneInfo.getCount() + 1);
                if (cmp > threshold) {
                    cloneInfo.setMetric(cloneInfo.getMetric() + cmp);
                    cloneInfo.getLinesFromFirstFile().add(
                            new Interval(((Java9Parser.BlockStatementsContext) tree1).getStart().getLine(),
                                    ((Java9Parser.BlockStatementsContext) tree1).getStop().getLine()));

                    cloneInfo.getLinesFromSecondFile().add(
                            new Interval(((Java9Parser.BlockStatementsContext) tree2).getStart().getLine(),
                                    ((Java9Parser.BlockStatementsContext) tree2).getStop().getLine()));
                }
            }
        }
        for (int i = 0; i < tree1.getChildCount(); i++) {
            for (int j = 0; j < tree2.getChildCount(); j++) {
                compareTrees(tree1.getChild(i), tree2.getChild(j), threshold, cloneInfo);
            }
        }
        return cloneInfo;
    }
}
