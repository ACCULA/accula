package org.accula.api.db.model;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

/**
 * @author Vadim Dyachkov
 */
@Data
@Builder
public class Clone {
    @Id
    @Nullable
    private Long id;
    private String targetCommitSha;
    private String targetFile;
    private Integer targetFromLine;
    private Integer targetToLine;
    private String sourceCommitSha;
    private String sourceFile;
    private Integer sourceFromLine;
    private Integer sourceToLine;
    @Builder.Default
    private Boolean suppressed = Boolean.FALSE;
}
