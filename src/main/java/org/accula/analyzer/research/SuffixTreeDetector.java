package org.accula.analyzer.research;

import com.suhininalex.suffixtree.SuffixTree;
import lombok.RequiredArgsConstructor;
import org.accula.parser.FileEntity;
import org.accula.parser.Parser;
import org.accula.parser.Token;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SuffixTreeDetector implements CloneDetector<FileEntity> {
    private final int minCloneLength;

    @Override
    public Flux<CloneInfo> findClones(final Flux<FileEntity> files) {
        final var tree = new SuffixTree<Token>();
        return files
                .flatMap(Parser::getFunctionsAsTokens)
                .map(tree::addSequence)
                .thenMany(analyze(tree));
    }

    private Flux<CloneInfo> analyze(final SuffixTree<Token> tree) {
        final List<CloneInfo> clones = new ArrayList<>();

        return Mono
                .fromRunnable(() -> {
                            final var start = System.nanoTime();

                            final var terminalNodes = SuffixTreeUtils.dfs(tree.getRoot());
                            final var reversedLinks = SuffixTreeUtils.reverseSuffixLink(terminalNodes);
                            final var siblings = new HashMap<Clone, List<Clone>>();
                            terminalNodes
                                    .stream()
                                    .filter(node -> {
                                        final var parentEdges = SuffixTreeUtils.getParentEdges(node);
                                        if (parentEdges.size() > 0) {
                                            final var lastEdge = parentEdges.get(parentEdges.size() - 1);
                                            return lastEdge.getBegin() == 1;
                                        } else return false;
                                    })
                                    .flatMap(node -> SuffixTreeUtils.getAllowedClones(node, reversedLinks))
                                    .map(SuffixTreeUtils::extractCloneInfo)
                                    .forEach(cloneInfo -> siblings
                                            .computeIfAbsent(cloneInfo.getReal(), _it -> new ArrayList<>())
                                            .add(cloneInfo.getClone()));
                            siblings
                                    .keySet()
                                    .stream()
                                    .flatMap(clone -> siblings.get(clone).stream().map(s -> new CloneInfo(clone, s)))
                                    .filter(this::filterByOwnerAndLength)
                                    .forEach(clones::add);

                            System.err.println(
                                    "Suffix tree clone detection time : " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms."
                            );
                        }
                )
                .thenMany(Flux.fromIterable(clones));
    }

    private boolean filterByOwnerAndLength(@NotNull final CloneInfo cloneInfo) {
        return !Objects.equals(cloneInfo.getClone().getOwner(), cloneInfo.getReal().getOwner())
                && cloneInfo.getClone().getLength() > minCloneLength;
    }
}
