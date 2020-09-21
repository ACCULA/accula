package org.accula.api.clone.suffixtree;

import com.suhininalex.suffixtree.SuffixTree;
import org.accula.api.token.TraverseUtils;
import org.accula.api.util.Sync;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
public final class SuffixTreeCloneDetector<Token extends Comparable<Token>, Ref> {
    private final SuffixTree<Token> suffixTree = new SuffixTree<>();
    private final Sync sync = new Sync();

    public void addTokens(final List<Token> tokens) {
        sync.writing(() -> {
            suffixTree.addSequence(tokens);
        });
    }

    public List<CloneClass<Ref>> cloneClassesAfterTransform(final Function<Stream<CloneClass<Ref>>, Stream<CloneClass<Ref>>> transform) {
        return sync.reading(() ->
                transform.apply(cloneClasses())
                        .collect(toList()))
                .get();
    }

    private Stream<CloneClass<Ref>> cloneClasses() {
        return TraverseUtils
                .dfs(suffixTree.getRoot(), SuffixTreeUtils::terminalNodes)
                .filter(SuffixTreeUtils::isCloneNode)
                .map(CloneClass<Ref>::new)
                .filter(cloneClass -> !cloneClass.getClones().isEmpty());
    }
}
