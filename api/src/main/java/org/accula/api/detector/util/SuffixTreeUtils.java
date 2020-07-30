package org.accula.api.detector.util;

import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.EndToken;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.detector.CodeSnippet;

import java.util.stream.Stream;

/**
 * @author VanyaKrylov
 */
@Slf4j
public final class SuffixTreeUtils {
    private SuffixTreeUtils() {
    }

    public static CodeSnippet getCodeSnippetFromEdge(final Edge edge, final int cloneLength) {
        final int sequenceEndIndex = edge.getBegin();
        final Token begin = (Token) edge.getSequence().get(sequenceEndIndex - cloneLength);
        final Token end = (Token) edge.getSequence().get(sequenceEndIndex - 1);

        //@formatter:off
        return new CodeSnippet((CommitSnapshot) begin.getCommitSnapshot(),
                                                begin.getFilename(),
                                                begin.getLine(),
                                                end.getLine());
        //@formatter:on
    }

    public static Stream<Edge> edgesFromTreeCloneClassForMethod(final TreeCloneClass treeCloneClass, final Long methodId) {
        return treeCloneClass.getTreeNode().getEdges()
                .stream()
                .filter(edge -> matchesToMethod(edge, methodId));
    }

    public static boolean matchesToMethod(final Edge edge, final Long methodId) {
        if (edge.getSequence().isEmpty()) {
            log.warn("Empty sequence for edge: " + edge.toString());
            return false;
        }
        final int lastElementIndex = edge.getSequence().size() - 1;
        final var lastElement = edge.getSequence().get(lastElementIndex);

        return lastElement instanceof EndToken && methodId.equals(((EndToken) lastElement).getIdSequence());
    }

    public static Token extractBeginToken(final TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getFirstElement();
    }

    public static Token extractEndToken(final TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getLastElement();
    }
}
