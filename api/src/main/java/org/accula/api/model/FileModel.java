package org.accula.api.model;

public class FileModel {
    private String filename;
    private String status;
    private String blob_url;
    private String patch;


    public FileModel (){
        super();
    }

    public FileModel (String filename, String status, String blob_url, String patch) {
        this.filename = filename;
        this.status = status;
        this.blob_url = blob_url;
        this.patch = patch;
    }

    // only for debug
    public String getAll(){
        return "filename: " + filename + "\n" +
                "status: " + status + "\n" +
                "blob_url: " + blob_url + "\n" +
                "patch: " + patch + "\n";
    }

    public String getFilename() { return filename; }
    public String getStatus() { return status; }
    public String getBlob_url() { return blob_url; }
    public String getPatch() { return patch; }
}
