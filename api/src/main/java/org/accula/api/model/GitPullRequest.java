package org.accula.api.model;

import lombok.Data;

@Data
public class GitPullRequest {
    private Integer number;
    private String url;
    private GitUserModel user;
}
