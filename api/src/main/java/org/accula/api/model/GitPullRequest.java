package org.accula.api.model;

public class GitPullRequest {
    private Integer number;
    private String url;
    private GitUserModel user;

    public GitPullRequest(){
        super();
    }

    public void setUser(final GitUserModel user){
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
