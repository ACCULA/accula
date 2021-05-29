package org.accula.api.clone.suffixtree;

import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.Node;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.accula.api.token.Token;
import org.accula.api.token.TraverseUtils;
import org.accula.api.util.Lambda;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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
        return root
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
            .filter(path -> path.getTerminal() == null)
            .collect(
                Object2IntOpenHashMap::new,
                (map, path) -> {
                    final var offset = map.getOrDefault(path.getParent().getParentEdge(), 0);
                    map.put(path, offset + length(path));
                },
                Lambda.illegalState()
            );
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

    static boolean isCloneNode(final Node node) {
        return node
            .getEdges()
            .stream()
            .map(Edge::getTerminal)
            .allMatch(Objects::isNull);
    }

    static <Ref> Map<Node, CloneClass<Ref>> superclassesByClassNode(final Collection<CloneClass<Ref>> cloneClasses) {
         return cloneClasses
            .stream()
            .filter(cloneClass -> cloneClass.node().getSuffixLink() != null)
            .collect(
                () -> new IdentityHashMap<>(cloneClasses.size()),
                (map, cloneClass) -> map.put(cloneClass.node().getSuffixLink(), cloneClass),
                Lambda.illegalState()
            );
    }
}
