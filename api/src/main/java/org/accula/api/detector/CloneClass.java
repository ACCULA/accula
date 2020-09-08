package org.accula.api.detector;

import com.suhininalex.suffixtree.Node;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
@Value
public class CloneClass {
    @ToString.Exclude
    Node node;
    @Getter(lazy = true)
    int length = length();
    @ToString.Exclude
    @Getter(lazy = true)
    List<Clone> clones = clones();

    private int length() {
        return SuffixTreeUtils.parentEdges(node)
                .mapToInt(SuffixTreeUtils::length)
                .sum();
    }

    private List<Clone> clones() {
        return SuffixTreeUtils.terminalMap(node)
                .entrySet()
                .stream()
                .map(entry -> {
                    final var edge = entry.getKey();
                    final var offset = entry.getValue();
                    final var to = edge.getEnd() - offset;
                    final var from = to - getLength() + 1;
                    return Clone.builder()
                            .parent(this)
                            .end(SuffixTreeUtils.get(edge, to))
                            .start(SuffixTreeUtils.get(edge, from))
                            .build();
                })
                .collect(toList());
    }
}
