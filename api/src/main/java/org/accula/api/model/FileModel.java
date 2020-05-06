package org.accula.api.model;

import java.util.Date;

public class FileModel {
    private String filename;
    private String status;
    private String blob_url;
    private String contents_url;
    private GitUserModel user_info;
    private Date pr_date;
    private String content;


    public FileModel(){
        super();
    }

    public FileModel(final String filename, final String status,
                     final String blob_url, final String contents_url,
                     final GitUserModel user_info,
                     final Date pr_date, final String content) {
        this.filename = filename;
        this.status = status;
        this.blob_url = blob_url;
        this.contents_url = contents_url;
        this.user_info = user_info;
        this.pr_date = pr_date;
        this.content = content;
    }

    // only for debug
    public String getAll(){
        return "filename: " + filename + "\n" +
                "status: " + status + "\n" +
                "blob_url: " + blob_url + "\n" +
                "contents_url: " + contents_url + "\n" +
                "user_info: " + user_info.getAll() + "\n" +
                "pr_date: " + pr_date + "\n" +
                "content: " + content + "\n";
    }

    public String getFilename() { return filename; }

    public String getStatus() { return status; }

    public String getBlob_url() { return blob_url; }

    public String getContents_url() { return contents_url; }

    public GitUserModel getUser_info() { return user_info; }

    public Date getPr_date() { return pr_date; }

    public String getContent() { return content; }

}
