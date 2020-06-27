package org.accula.analyzer;

import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.Node;
import org.accula.parser.Token;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SuffixTreeUtils {
    /**
     * Get nodes with edges that contain a terminal using depth-first-search.
     *
     * @param node start node
     * @return list of nodes
     */
    public static List<Node> dfs(@NotNull final Node node) {
        final var list = new LinkedList<Node>();
        list.add(node);
        node
                .getEdges()
                .stream()
                .filter(edge -> edge.getTerminal() != null)
                .map(edge -> dfs(edge.getTerminal()))
                .forEach(list::addAll);
        return list;
    }

    /**
     * Get edges from the current node to the root.
     *
     * @param node start node
     * @return list of edges
     */
    public static List<Edge> getParentEdges(@NotNull final Node node) {
        final var list = new LinkedList<Edge>();
        final var parentEdge = node.getParentEdge();
        if (parentEdge != null) {
            list.add(parentEdge);
            list.addAll(getParentEdges(parentEdge.getParent()));
        }
        return list;
    }

    /**
     * Get possible clones from a given node.
     * If node contains edges with only EndToken,
     * this node should be a leaf to reduce the number of duplicated clones.
     * Now this is achieved using a weird filter.
     *
     * @param node current node
     * @return stream of clones
     */
    public static Stream<List<Edge>> getClonesFromNode(@NotNull final Node node) {
        final var parentEdges = getParentEdges(node);
        return node
                .getEdges()
                .stream()
                .filter(edge -> edge.getTerminal() == null)
                .map(edge -> {
                    final List<Edge> tmp = new LinkedList<>();
                    tmp.add(edge);
                    tmp.addAll(parentEdges);
                    return tmp;
                })
                .filter(list ->
                        list.get(0).getP() != list.get(0).getK() && list.size() > 1 ||
                                list.get(0).getP() == list.get(0).getK() && list.size() > 2);
    }

    public static Map<Node, Node> reverseSuffixLink(@NotNull final List<Node> nodes) {
        final var map = new IdentityHashMap<Node, Node>();
        nodes
                .stream()
                .filter(it -> it.getSuffixLink() != null)
                .forEach(it -> map.put(it.getSuffixLink(), it));
        return map;
    }

    /**
     * Check if there are other nodes with similar clones.
     * https://github.com/suhininalex/IdeaClonePlugin/blob/master/src/main/com/suhininalex/clones/core/postprocessing/SubClassFilter.kt#L21
     *
     * @param node                parent node
     * @param reversedSuffixLinks map of reversed suffix links
     * @return true if there are no similar clones with a greater number of tokens (?)
     */
    public static boolean isAllowed(@NotNull final Node node,
                                    @NotNull final Map<Node, Node> reversedSuffixLinks) {
        final var greaterNode = reversedSuffixLinks.get(node);
        if (greaterNode == null) return true;
        final var prevEdgesGreaterNode = SuffixTreeUtils.getParentEdges(greaterNode).get(0);
        final var prevEdgesCurrentNode = SuffixTreeUtils.getParentEdges(node).get(0);

        return prevEdgesGreaterNode.getEnd() < prevEdgesCurrentNode.getEnd();
    }

    /**
     * Map list of edges to CloneInfo.
     * The first element of the input list contains cloned sequence.
     * The second element of the input list contains the sequence that was cloned.
     *
     * @param cloneEdges list of edges
     * @return information about the clone
     */
    public static CloneInfo extractCloneInfo(@NotNull List<Edge> cloneEdges) {
        final var firstEdgeSeq = cloneEdges.get(0).getSequence();
        final var lastEdgeSeq = cloneEdges.get(1).getSequence();

        final var clone = new Clone(
                (Token) firstEdgeSeq.get(cloneEdges.get(cloneEdges.size() - 1).getBegin()),
                (Token) firstEdgeSeq.get(cloneEdges.get(1).getEnd() - 1)
        );
        final var cloneFrom = new Clone(
                (Token) lastEdgeSeq.get(cloneEdges.get(cloneEdges.size() - 1).getBegin()),
                (Token) lastEdgeSeq.get(cloneEdges.get(1).getEnd() - 1)
        );

        return new CloneInfo(clone, cloneFrom);
    }
}
