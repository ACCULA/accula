package org.accula.api.model;

public class GitUserModel {
    private String login;
    private String html_url;

    public GitUserModel(){
        super();
    }

    // only for debug
    public String getAll(){
        return "login: " + login + "\n" +
                "html_url: " + html_url + "\n";
    }

    public String getLogin() { return login; }

    public String getHtml_url() { return html_url; }
}
