package org.accula.api.model;

import java.util.Date;

public class GitPullRequest {
    private String url;
    private GitUserModel user;
    private Date created_at;
    private String title;

    public GitPullRequest(){
        super();
    }

    public String getAll() {
        return "title: " + title + "\n" +
                "url: " + url + "\n" +
                "created_at: " + created_at + "\n" +
                "user: {\n" + user.getAll() + "}\n";
    }

    public GitUserModel getUser() { return user; }

    public String getUrl() { return url; }

    public Date getCreated_at() { return created_at; }

    public String getTitle() { return title; }
}
