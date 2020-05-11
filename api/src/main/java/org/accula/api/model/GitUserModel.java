package org.accula.api.model;

import org.springframework.lang.Nullable;

public class GitUserModel {
    private String login;
    private String url;
    @Nullable
    private String name;


    public GitUserModel(){
        super();
    }

    public String getLogin() {
        return login;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
