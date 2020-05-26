package org.accula.api.detector;

import org.accula.api.code.FileEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.List;

public class CloneDetectorImpl implements CloneDetector {
    @Override
    public MultiValueMap<CodeSnippet, CodeSnippet> findClones(Collection<FileEntity> filesToCheck, Collection<FileEntity> otherFiles) {
        MultiValueMap<String, CodeSnippet> source = new LinkedMultiValueMap<>();
        MultiValueMap<CodeSnippet, CodeSnippet> clones = new LinkedMultiValueMap<>();
        for (FileEntity file : filesToCheck) {
            String[] lines = file.getContent().split("\n");
            for (int i = 1; i <= lines.length; i++) {
                String line = lines[i - 1];
                CodeSnippet snippet = new CodeSnippet(file.getCommit(), file.getName(), i, i);
                source.add(line, snippet);
            }
        }
        for (FileEntity file : otherFiles) {
            String[] lines = file.getContent().split("\n");
            for (int i = 1; i <= lines.length; i++) {
                String line = lines[i - 1];
                if (source.containsKey(line)) {
                    CodeSnippet snippet = new CodeSnippet(file.getCommit(), file.getName(), i, i);
                    List<CodeSnippet> snippets = source.get(line);
                    assert snippets != null;
                    snippets.forEach(s -> clones.add(s, snippet));
                }
            }
        }
        return clones;
    }
}
