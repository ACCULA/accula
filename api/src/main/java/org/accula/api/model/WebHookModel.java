package org.accula.api.model;

public class WebHookModel {
    private GitPullRequest pull_request;

    public WebHookModel(){
        super();
    }

    public GitPullRequest getPull_request() { return pull_request; }
}
