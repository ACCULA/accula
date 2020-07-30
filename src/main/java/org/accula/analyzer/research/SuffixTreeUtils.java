package org.accula.analyzer.research;

import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.Node;
import org.accula.parser.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SuffixTreeUtils {
    /**
     * Get nodes with edges that contain a terminal using depth-first-search.
     *
     * @param node start node
     * @return list of nodes
     */
    public static List<Node> dfs(@NotNull final Node node) {
        final var list = new ArrayList<Node>();
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

    public static Map<Node, Long> reverseSuffixLink(@NotNull final List<Node> nodes) {
        final var map = new IdentityHashMap<Node, Long>();
        nodes
                .stream()
                .filter(node -> node.getSuffixLink() != null)
                .forEach(node -> {
                    final var numberOfClones = getClonesFromNode(node).count();
                    map.put(node.getSuffixLink(), numberOfClones);
                });
        return map;
    }

    /**
     * Get allowed clones checking if there are no other nodes with similar clones.
     * https://github.com/suhininalex/IdeaClonePlugin/blob/master/src/main/com/suhininalex/clones/core/postprocessing/SubClassFilter.kt#L21
     *
     * @param node                parent node
     * @param reversedSuffixLinks map of reversed suffix links
     * @return stream of clones if there are no similar clones
     */
    public static Stream<List<Edge>> getAllowedClones(@NotNull final Node node,
                                                      @NotNull final Map<Node, Long> reversedSuffixLinks) {
        final var currentNodeClones = getClonesFromNode(node).collect(Collectors.toUnmodifiableList());
        final var greaterNodeClonesSize = reversedSuffixLinks.get(node);
        if (greaterNodeClonesSize == null) return currentNodeClones.stream();
        if (greaterNodeClonesSize != currentNodeClones.size())
            return currentNodeClones.stream();
        else
            return Stream.empty();
    }

    private static int getCloneLength(@NotNull final List<Edge> cloneEdges) {
        return cloneEdges
                .stream()
                .skip(1)
                .mapToInt(edge -> edge.getEnd() - edge.getBegin() + 1)
                .sum();
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
        final var firstEdge = cloneEdges.get(0);
        final var secondEdge = cloneEdges.get(1);
        final var length = getCloneLength(cloneEdges);

        final var clone = new Clone(
                (Token) firstEdge.getSequence().get(firstEdge.getBegin() - length),
                (Token) firstEdge.getSequence().get(firstEdge.getBegin() - 1),
                length
        );
        final var cloneFrom = new Clone(
                (Token) secondEdge.getSequence().get(secondEdge.getEnd() - length + 1),
                (Token) secondEdge.getSequence().get(secondEdge.getEnd()),
                length
        );

        return new CloneInfo(clone, cloneFrom);
    }
}
