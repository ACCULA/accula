package org.accula.api.model;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class PullRequestModel {
    private String url;
    private GitUserModel user;
    private Date created_at;
    private String title;
    private List<FileModel> changed_files;

    public PullRequestModel(){
        super();
    }

    public PullRequestModel (String title, Date created_at, GitUserModel user,
                            String url) {
        this.url = url;
        this.user = user;
        this.created_at = created_at;
        this.title = title;
    }

    // Only for testing goals - makes string from class
    public String getAll(){
        Iterator<FileModel> a = changed_files.iterator();
        String files = "";
        while (a.hasNext()){
            files += a.next().getAll() + "\n";
        }
        return "title: " + title + "\n" +
                "url: " + url + "\n" +
                "created_at: " + created_at + "\n" +
                "user: {\n" + user.getAll() + "}\n" +
                "changed_files: { " + files + "}\n";
    }

    public void setChanged_files(List<FileModel> changed_files) {
        this.changed_files = changed_files;
    }

    public GitUserModel getUser() { return user; }
    public String getUrl() { return url; }
    public Date getCreated_at() { return created_at; }
    public String getTitle() { return title; }
    public List<FileModel> getChanged_files() { return changed_files; }

}
