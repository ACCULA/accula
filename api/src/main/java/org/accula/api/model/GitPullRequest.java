package org.accula.api.model;
import org.springframework.lang.Nullable;

public class GitPullRequest {
    private Integer number;
    private String url;
    private GitUserModel user;

    public GitPullRequest(){
        super();
    }

    public void setUser(GitUserModel user){
        this.user = user;
    }

    public GitUserModel getUser() {
        return user;
    }

    public String getUrl() {
        return url;
    }

    public Integer getNumber(){
        return number;
    }

}
