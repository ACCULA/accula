package org.accula.api.clone.suffixtree;

import com.google.common.collect.Streams;
import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.Node;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.accula.api.token.Token;
import org.accula.api.token.TraverseUtils;

import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Anton Lamtev
 */
final class SuffixTreeUtils {
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

    static Object2IntMap<Edge> terminalMap(final Node root) {
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

        final Object2IntMap<Edge> terminalMap = paths
                .stream()
                .collect(Object2IntOpenHashMap::new, (map, path) -> {
                    final var offset = map.getOrDefault(path.getParent().getParentEdge(), 0);
                    map.put(path, offset + length(path));
                }, Object2IntMap::putAll);

        final var ends = paths
                .stream()
                .filter(path -> path.getTerminal() == null)
                .collect(toSet());

        terminalMap.object2IntEntrySet().removeIf(entry -> !ends.contains(entry.getKey()));

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

    //TODO: убедиться в корректности. На данный момент известно,
    // что при таком критерии часть классов клонов может быть отсечена (хотя вроде по-хорошему не должна была)
    @SuppressWarnings("UnstableApiUsage")
    static boolean isCloneNode(final Node node) {
        final var allEdgesAreTerminal = node
                .getEdges()
                .stream()
                .allMatch(SuffixTreeUtils::isTerminalEdge);
        return allEdgesAreTerminal && Streams.findLast(SuffixTreeUtils.parentEdges(node))
                .filter(edge -> edge.getBegin() == 0)
                .isPresent();
    }

    static boolean isTerminalEdge(final Edge edge) {
        return edge.getBegin() == edge.getEnd() && edge.getBegin() == edge.getSequence().size() - 1;
    }
}
