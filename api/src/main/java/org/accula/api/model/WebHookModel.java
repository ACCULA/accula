package org.accula.api.model;

public class WebHookModel {
    private GitPullRequest pull_request;

    public WebHookModel(){
        super();
    }

    public WebHookModel(GitPullRequest pull_request) {
        this.pull_request = pull_request;
    }

    public GitPullRequest getPull_request() { return pull_request; }
}
