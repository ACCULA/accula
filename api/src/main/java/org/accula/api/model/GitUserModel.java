package org.accula.api.model;

import java.util.Date;

public class GitUserModel {
    private String login;
    private String html_url;

    public GitUserModel(){
        super();
    }

    public GitUserModel (String login, String html_url) {
        this.login = login;
        this.html_url = html_url;
    }

    // only for debug
    public String getAll(){
        return "login: " + login + "\n" +
                "html_url: " + html_url + "\n";
    }

    public String getLogin() { return login; }
    public String getHtml_url() { return html_url; }
}
