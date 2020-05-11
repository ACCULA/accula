package org.accula.api.model;

import java.util.Date;

public class FileModel {
    private String filename;
    private String filePath;
    private String prUrl;
    private String userName;
    private Date changedAt;
    private String content;

    public FileModel(){
        super();
    }

    public FileModel(final String filename,final String filePath, final String prUrl,
                     final String userName, final Date changedAt,
                     final String content) {
        this.filename = filename;
        this.filePath = filePath;
        this.prUrl = prUrl;
        this.userName = userName;
        this.changedAt = changedAt;
        this.content = content;
    }

    public String getFilename() {
        return filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public String getUserName() {
        return userName;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public String getContent() {
        return content;
    }
}
