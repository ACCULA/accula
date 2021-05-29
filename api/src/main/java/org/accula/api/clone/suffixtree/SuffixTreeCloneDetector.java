package org.accula.api.clone.suffixtree;

import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.token.TraverseUtils;
import org.accula.api.util.Sync;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
@Slf4j
public final class SuffixTreeCloneDetector<Token extends Comparable<Token>, Ref> {
    private final SuffixTree<Token> suffixTree = new SuffixTree<>();
    private final Sync sync = new Sync();

    public SuffixTreeCloneDetector() {
        Flux.interval(Duration.ofMinutes(5L), Schedulers.single())
                .doOnNext(next ->
                        log.info("Node count = {}", nodeCount()))
                .name("Suffix tree node count timer")
                .subscribe();
    }

    public long addTokens(final List<Token> tokens) {
        return sync.write(() -> suffixTree.addSequence(tokens));
    }

    public List<CloneClass<Ref>> cloneClasses(final Predicate<CloneClass<Ref>> filter) {
        return cloneClasses(it -> true, filter);
    }

    public List<CloneClass<Ref>> cloneClasses(final Predicate<CloneClass<Ref>> cheapFilter,
                                              final Predicate<CloneClass<Ref>> expensiveFilter) {
        final var cloneClasses =  sync.read(
            () -> cloneClasses()
                .filter(cheapFilter)
                .toList()
        );

        final var superclassesByClassNode = SuffixTreeUtils.superclassesByClassNode(cloneClasses);

        return cloneClasses
            .stream()
            .filter(cloneClass -> {
                final var superclass = superclassesByClassNode.get(cloneClass.node());
                if (superclass == null) {
                    return true;
                }
                return cloneClass.cloneCount() > superclass.cloneCount();
            })
            .filter(expensiveFilter)
            .toList();
    }

    private Stream<CloneClass<Ref>> cloneClasses() {
        return TraverseUtils
                .dfs(suffixTree.getRoot(), SuffixTreeUtils::terminalNodes)
                .filter(SuffixTreeUtils::isCloneNode)
                .map(CloneClass<Ref>::new);
    }

    private long nodeCount() {
        return sync.read(() ->
            TraverseUtils
                .dfs(suffixTree.getRoot(), node -> node
                    .getEdges()
                    .stream()
                    .map(Edge::getTerminal)
                    .filter(Objects::nonNull)
                )
                .count()
        );
    }
}
