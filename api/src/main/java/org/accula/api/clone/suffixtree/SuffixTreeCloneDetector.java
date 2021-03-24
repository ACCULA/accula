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
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
@Slf4j
public final class SuffixTreeCloneDetector<Token extends Comparable<Token>, Ref> {
    private final SuffixTree<Token> suffixTree = new SuffixTree<>();
    private final Sync sync = new Sync();

    public SuffixTreeCloneDetector() {
        Flux.interval(Duration.ofMinutes(1L), Schedulers.single())
                .doOnNext(next ->
                        log.info("Node count = {}", sync.read(() ->
                                TraverseUtils.dfs(suffixTree.getRoot(), it ->
                                        it.getEdges()
                                                .stream()
                                                .map(Edge::getTerminal)
                                                .filter(Objects::nonNull))
                                        .count())))
                .name("Suffix tree node count timer")
                .subscribe();
    }

    public long addTokens(final List<Token> tokens) {
        return sync.write(() -> suffixTree.addSequence(tokens));
    }

    public <R> List<R> transform(final Function<Stream<CloneClass<Ref>>, Stream<R>> transform) {
        return sync.read(() ->
                transform.apply(cloneClasses())
                        .collect(toList()));
    }

    //TODO: Отфильтровывать те классы, которые являются подклассами других, более больших классов,
    // и при этом среди клонов этих подклассов, нет новых по сравнению с суперклассами:
    // по суффиксным ссылкам через вершины ищем подклассы. затем смотрим на клоны внутри подклассов.
    // Классов клонов будет очень много, поэтому выполнять данную фильтрацию стоит, наверное,
    // уже после отсечения лишних классов по более "дешевым" признакам типа количества токенов, отношения к целевому ПР и тд
    private Stream<CloneClass<Ref>> cloneClasses() {
        return TraverseUtils
                .dfs(suffixTree.getRoot(), SuffixTreeUtils::terminalNodes)
                .filter(SuffixTreeUtils::isCloneNode)
                .map(CloneClass<Ref>::new)
                .filter(cloneClass -> !cloneClass.clones().isEmpty());
    }
}
