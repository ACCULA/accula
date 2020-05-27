package org.accula.api.detector;

import org.accula.api.code.FileEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CloneDetectorImpl implements CloneDetector {
    public static int MIN_LINE_LENGTH = 20;

    @Override
    public MultiValueMap<CodeSnippet, CodeSnippet> findClones(final Collection<FileEntity> filesToCheck,
                                                              final Collection<FileEntity> otherFiles) {
        final MultiValueMap<String, CodeSnippet> source = new LinkedMultiValueMap<>();
        for (final FileEntity file : filesToCheck) {
            String[] lines = file.getContent().split("\n");
            for (int i = 1; i <= lines.length; i++) {
                final String line = lines[i - 1];
                if (line.isBlank() || line.length() < MIN_LINE_LENGTH) {
                    continue;
                }
                final CodeSnippet snippet = new CodeSnippet(file.getCommit(), file.getName(), i, i);
                source.add(line, snippet);
            }
        }

        final MultiValueMap<CodeSnippet, CodeSnippet> clones = new LinkedMultiValueMap<>();
        for (final FileEntity file : otherFiles) {
            final String[] lines = file.getContent().split("\n");
            for (int i = 1; i <= lines.length; i++) {
                final String line = lines[i - 1];
                if (line.isBlank() || line.length() < MIN_LINE_LENGTH) {
                    continue;
                }
                if (source.containsKey(line)) {
                    final CodeSnippet snippet = new CodeSnippet(file.getCommit(), file.getName(), i, i);
                    final List<CodeSnippet> snippets = source.get(line);
                    assert snippets != null;
                    snippets.forEach(s -> clones.add(s, snippet));
                }
            }
        }

        return tryMerge(clones);
    }
    
    private MultiValueMap<CodeSnippet, CodeSnippet> tryMerge(final MultiValueMap<CodeSnippet, CodeSnippet> clones) {
        if (clones.isEmpty()) {
            return clones;
        }
        final MultiValueMap<CodeSnippet, CodeSnippet> result = new LinkedMultiValueMap<>();
        var it = clones.entrySet().iterator();
        var prev = it.next();
        while (it.hasNext()) {
            final var curr = it.next();
            final CodeSnippet firstKey = prev.getKey();
            final CodeSnippet secondKey = curr.getKey();
            if (isMergeable(firstKey, secondKey)) {
                if (prev.getValue().size() == 1 && curr.getValue().size() == 1) {
                    final CodeSnippet firstClone = prev.getValue().get(0);
                    final CodeSnippet secondClone = curr.getValue().get(0);
                    if (isMergeable(firstClone, secondClone)) {
                        final CodeSnippet mergedKey = merge(firstKey, secondKey);
                        final CodeSnippet mergedValue = merge(firstClone, secondClone);
                        result.remove(firstKey);
                        result.add(mergedKey, mergedValue);
                        prev = Map.entry(mergedKey, List.of(mergedValue));
                        continue;
                    }
                }
            }
            result.put(firstKey, prev.getValue());
            prev = curr;
        }
        result.put(prev.getKey(), prev.getValue());
        return result;
    }
    
    private boolean isMergeable(final CodeSnippet first, final CodeSnippet second) {
        return first.getCommit().equals(second.getCommit())
                && first.getFile().equals(second.getFile())
                && first.getToLine() + 1 == second.getFromLine();
    }
    
    private CodeSnippet merge(final CodeSnippet first, final CodeSnippet second) {
        assert first.getCommit().equals(second.getCommit());
        assert first.getFile().equals(second.getFile());
        return new CodeSnippet(first.getCommit(), first.getFile(), first.getFromLine(), second.getToLine());
    }
}
