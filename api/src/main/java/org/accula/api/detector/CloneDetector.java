package org.accula.api.detector;

import org.accula.api.code.FileEntity;
import org.springframework.util.MultiValueMap;

import java.util.Collection;

public interface CloneDetector {
    MultiValueMap<CodeSnippet, CodeSnippet> findClones(Collection<FileEntity> filesToCheck,
                                                       Collection<FileEntity> otherFiles);
}
