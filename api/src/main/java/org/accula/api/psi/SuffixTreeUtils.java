package org.accula.api.psi;

import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Anton Lamtev
 */
public final class SuffixTreeUtils {
    private SuffixTreeUtils() {
    }

    public static Stream<Edge> parentEdges(final Node root) {
        final var parentEdge = root.getParentEdge();
        if (parentEdge == null) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.of(parentEdge),
                parentEdges(parentEdge.getParent()));
    }

    public static int length(final Edge edge) {
        return edge.getEnd() - edge.getBegin() + 1;
    }

    public static Map<Edge, Integer> terminalMap(final Node root) {
        final var paths = root
                .getEdges()
                .stream()
                .flatMap(edge -> TraverseUtils.dfs(edge, e -> {
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

    public static Token get(final Edge edge, final int index) {
        return (Token) edge.getSequence().get(index);
    }
}
