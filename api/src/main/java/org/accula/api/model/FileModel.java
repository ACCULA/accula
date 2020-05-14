package org.accula.api.model;

import lombok.Data;

import java.util.Date;

@Data
public class FileModel {
    private String filename;
    private String filePath;
    private String prUrl;
    private String userName;
    private Date changedAt;
    private String content;

    public FileModel(final String filename, final String filePath, final String prUrl,
                     final String userName, final Date changedAt,
                     final String content) {
        this.filename = filename;
        this.filePath = filePath;
        this.prUrl = prUrl;
        this.userName = userName;
        this.changedAt = changedAt;
        this.content = content;
    }
}
