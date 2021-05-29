package org.accula.api.clone.suffixtree;

import com.suhininalex.suffixtree.Node;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.util.List;

/**
 * @author Anton Lamtev
 */
@Value
public class CloneClass<Ref> {
    @ToString.Exclude
    Node node;
    @Getter(lazy = true)
    int length = computeLength();
    @ToString.Exclude
    @Getter(lazy = true)
    List<Clone<Ref>> clones = traverseClones();

    public int cloneCount() {
        return clones().size();
    }

    private int computeLength() {
        return SuffixTreeUtils.parentEdges(node)
                .mapToInt(SuffixTreeUtils::length)
                .sum();
    }

    private List<Clone<Ref>> traverseClones() {
        return SuffixTreeUtils.terminalMap(node)
                .object2IntEntrySet()
                .stream()
                .map(entry -> {
                    final var edge = entry.getKey();
                    final var offset = entry.getIntValue();
                    final var to = edge.getEnd() - offset;
                    final var from = to - length() + 1;
                    return Clone.<Ref>builder()
                            .parent(this)
                            .end(SuffixTreeUtils.get(edge, to))
                            .start(SuffixTreeUtils.get(edge, from))
                            .build();
                })
                .toList();
    }
}
