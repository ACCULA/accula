package org.accula.api.model;

import java.util.Date;

public class WebHookModel {
    private GitPullRequest pull_request;

    public WebHookModel(){
        super();
    }

    public WebHookModel(GitPullRequest pull_request) {
        this.pull_request = pull_request;
    }

    public GitPullRequest getPull_request() { return pull_request; }
    public String getPRUrl() {return getPull_request().getUrl();}
    public PullRequestModel formPullRequestModel (){
        return new PullRequestModel(getPull_request().getTitle(), getPull_request().getCreated_at(),
                getPull_request().getUser(), getPull_request().getUrl());
    }
}
