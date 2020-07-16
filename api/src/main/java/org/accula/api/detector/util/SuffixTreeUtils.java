package org.accula.api.detector.util;

import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.EndToken;
import lombok.NonNull;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.detector.CodeSnippet;

import java.util.stream.Stream;

public final class SuffixTreeUtils {

    private SuffixTreeUtils() {}

    public static CodeSnippet getCodeSnippetFromEdge(@NonNull final Edge edge, final int cloneLength) {
        int sequenceEndIndex = edge.getBegin();
        Token begin = (Token) edge.getSequence().get(sequenceEndIndex - cloneLength);
        Token end = (Token) edge.getSequence().get(sequenceEndIndex - 1);

        return new CodeSnippet((CommitSnapshot) begin.getCommitSnapshot(),
                begin.getFilename(),
                begin.getLine(),
                end.getLine());
    }

    public static Stream<Edge> edgesFromTreeCloneClassForMethod(@NonNull final TreeCloneClass treeCloneClass,
                                                                @NonNull final Long methodId) {
        return treeCloneClass.getTreeNode()
                .getEdges()
                .stream()
                .filter(edge -> matchesToMethod(edge, methodId));
    }

    public static boolean matchesToMethod(@NonNull final Edge edge, @NonNull final Long methodId) {
        final int lastElementIndex = edge.getSequence().size() - 1;
        final Object element = edge.getSequence().get(lastElementIndex);

        return (element instanceof EndToken endToken) && (endToken.getIdSequence() == methodId);
    }

    public static Token extractBeginToken(TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getFirstElement();
    }

    public static Token extractEndToken(TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getLastElement();
    }
}
