package org.accula.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class GitPullRequest {
    private Integer number;
    private String url;
    private GitUserModel user;
    @JsonProperty("created_at")
    private Date createdAt;
}
