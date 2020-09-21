package org.accula.api.clone.suffixtree;

import com.google.common.collect.Streams;
import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.Node;
import org.accula.api.token.Token;
import org.accula.api.token.TraverseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Anton Lamtev
 */
final class SuffixTreeUtils {
    private static final Integer ZERO = 0;

    private SuffixTreeUtils() {
    }

    static Stream<Edge> parentEdges(final Node root) {
        final var parentEdge = root.getParentEdge();
        if (parentEdge == null) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.of(parentEdge),
                parentEdges(parentEdge.getParent()));
    }

    static int length(final Edge edge) {
        return edge.getEnd() - edge.getBegin() + 1;
    }

    static Map<Edge, Integer> terminalMap(final Node root) {
        final var paths = root
                .getEdges()
                .stream()
                .flatMap(edge -> TraverseUtils
                        .dfs(edge, e -> {
                            final var terminal = e.getTerminal();
                            if (terminal == null) {
                                return Stream.empty();
                            }
                            return terminal.getEdges().stream();
                        }))
                .collect(toList());

        final Map<Edge, Integer> terminalMap = paths
                .stream()
                .collect(HashMap::new, (map, path) -> {
                    final var offset = map.get(path.getParent().getParentEdge());
                    map.put(path, (offset == null ? 0 : offset) + length(path));
                }, HashMap::putAll);

        final var ends = paths
                .stream()
                .filter(path -> path.getTerminal() == null)
                .collect(toSet());

        terminalMap.entrySet().removeIf(entry -> !ends.contains(entry.getKey()));

        return terminalMap;
    }

    @SuppressWarnings("unchecked")
    static <Ref> Token<Ref> get(final Edge edge, final int index) {
        return (Token<Ref>) edge.getSequence().get(index);
    }

    static Stream<Node> terminalNodes(final Node node) {
        return node
                .getEdges()
                .stream()
                .map(Edge::getTerminal)
                .filter(Objects::nonNull);
    }

    @SuppressWarnings("UnstableApiUsage")
    static boolean isCloneNode(final Node node) {
        final var allEdgesAreTerminal = node
                .getEdges()
                .stream()
                .allMatch(SuffixTreeUtils::isTerminalEdge);
        return allEdgesAreTerminal && Streams.findLast(SuffixTreeUtils.parentEdges(node))
                .map(Edge::getBegin)
                .filter(ZERO::equals)
                .isPresent();
    }

    static boolean isTerminalEdge(final Edge edge) {
        return edge.getBegin() == edge.getEnd() && edge.getBegin() == edge.getSequence().size() - 1;
    }
}
