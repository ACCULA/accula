package org.accula.api.detector;

import org.accula.api.code.FileEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vadim Dyachkov
 */
public class PrimitiveCloneDetector implements CloneDetector {
    private final int minLineLength;
    private final int minLineCount;

    public PrimitiveCloneDetector(final int minLineLength, final int minLineCount) {
        this.minLineLength = minLineLength;
        this.minLineCount = minLineCount;
    }

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles, final Flux<FileEntity> sourceFiles) {
        final Mono<Map<String, Collection<CodeSnippet>>> target = targetFiles
                .map(this::lineToSnippetsMap)
                .collectList()
                .map(PrimitiveCloneDetector::reduceMaps)
                .cache();
        return sourceFiles
                .flatMap(source -> target.zipWith(Mono.just(source)))
                .flatMap(targetAndSource -> Flux.fromIterable(findClonesInFile(targetAndSource.getT1(), targetAndSource.getT2())))
                .filter(targetAndSource -> targetAndSource.getT2().getLineCount() >= minLineCount);
    }

    private Map<String, Collection<CodeSnippet>> lineToSnippetsMap(final FileEntity file) {
        final Map<String, Collection<CodeSnippet>> source = new HashMap<>();
        final String[] lines = file.getContent().split("\n");
        for (int i = 1; i <= lines.length; i++) {
            final String line = lines[i - 1];
            if (line.length() < minLineLength) {
                continue;
            }
            final CodeSnippet snippet = new CodeSnippet(file.getCommitSnapshot(), file.getName(), i, i);
            add(source, line, snippet);
        }
        return source;
    }

    private List<Tuple2<CodeSnippet, CodeSnippet>> findClonesInFile(final Map<String, Collection<CodeSnippet>> target,
                                                                    final FileEntity file) {
        final List<Tuple2<CodeSnippet, CodeSnippet>> clones = new ArrayList<>();
        final String[] lines = file.getContent().split("\n");
        for (int i = 1; i <= lines.length; i++) {
            final String line = lines[i - 1];
            if (line.length() < minLineLength) {
                continue;
            }
            if (target.containsKey(line)) {
                final CodeSnippet source = new CodeSnippet(file.getCommitSnapshot(), file.getName(), i, i);
                final Collection<CodeSnippet> snippets = target.get(line);
                assert snippets != null;
                snippets.forEach(s -> clones.add(Tuples.of(s, source)));
            }
        }

        final var sorted = clones.stream()
                .sorted(Comparator.comparing(t -> t.getT1().toString()))
                .collect(Collectors.toList());
        return tryMerge(sorted);
    }

    private List<Tuple2<CodeSnippet, CodeSnippet>> tryMerge(final List<Tuple2<CodeSnippet, CodeSnippet>> clones) {
        if (clones.isEmpty()) {
            return clones;
        }
        final List<Tuple2<CodeSnippet, CodeSnippet>> result = new ArrayList<>();
        final Iterator<Tuple2<CodeSnippet, CodeSnippet>> it = clones.iterator();
        Tuple2<CodeSnippet, CodeSnippet> lastMerge = null;
        Tuple2<CodeSnippet, CodeSnippet> prev = it.next();
        Tuple2<CodeSnippet, CodeSnippet> curr;
        while (it.hasNext()) {
            curr = it.next();
            final CodeSnippet target1 = prev.getT1();
            final CodeSnippet target2 = curr.getT1();
            final CodeSnippet source1 = prev.getT2();
            final CodeSnippet source2 = curr.getT2();
            if (isMergeable(target1, target2) && isMergeable(source1, source2)) {
                final CodeSnippet mergedTarget = merge(target1, target2);
                final CodeSnippet mergedSource = merge(source1, source2);
                result.remove(lastMerge);
                lastMerge = Tuples.of(mergedTarget, mergedSource);
                result.add(lastMerge);
                prev = lastMerge;
            } else {
                if (prev != lastMerge) { // NOPMD
                    result.add(prev);
                }
                lastMerge = null; // NOPMD
                prev = curr;
            }
        }
        if (prev != lastMerge) { // NOPMD
            result.add(prev);
        }
        return result;
    }

    private static boolean isMergeable(final CodeSnippet first, final CodeSnippet second) {
        return first.getCommitSnapshot().equals(second.getCommitSnapshot())
                && first.getFile().equals(second.getFile())
                && first.getToLine() + 1 == second.getFromLine();
    }

    private static CodeSnippet merge(final CodeSnippet first, final CodeSnippet second) {
        assert first.getCommitSnapshot().equals(second.getCommitSnapshot());
        assert first.getFile().equals(second.getFile());
        return new CodeSnippet(first.getCommitSnapshot(), first.getFile(), first.getFromLine(), second.getToLine());
    }

    private static <K, V> Map<K, Collection<V>> reduceMaps(final List<Map<K, Collection<V>>> maps) {
        return maps
                .stream()
                .flatMap(s -> s.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    final List<V> temp = new ArrayList<>();
                    temp.addAll(a);
                    temp.addAll(b);
                    return temp;
                }));
    }

    private static <K, V> void add(final Map<K, Collection<V>> map, final K key, final V value) {
        map.putIfAbsent(key, new ArrayList<>());
        map.get(key).add(value);
    }
}
