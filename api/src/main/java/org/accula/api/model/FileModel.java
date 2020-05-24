package org.accula.api.model;

import lombok.Data;

import java.util.Date;

@Data
public class FileModel {
    private String filename;
    private String filePath;
    private String prUrl;
    private String userLogin;
    private Date prDate;
    private String content;

    public FileModel(final String filename, final String filePath, final String prUrl,
                     final String userLogin, final Date prDate,
                     final String content) {
        this.filename = filename;
        this.filePath = filePath;
        this.prUrl = prUrl;
        this.userLogin = userLogin;
        this.prDate = prDate;
        this.content = content;
    }
}
