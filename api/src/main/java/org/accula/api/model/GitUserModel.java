package org.accula.api.model;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class GitUserModel {
    private String login;
    private String url;
    @Nullable
    private String name;
}
