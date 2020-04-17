package org.accula.api.model;

import java.util.Iterator;
import java.util.List;

public class PullRequestModel {
    private GitPullRequest pull_request;
    private List<FileModel> changed_files;

    public PullRequestModel(){
        super();
    }

    public PullRequestModel(GitPullRequest pull_request, List<FileModel> changed_files) {
        this.pull_request = pull_request;
        this.changed_files = changed_files;
    }

    // Only for testing goals - makes string from class
    public String getAll(){
        Iterator<FileModel> a = changed_files.iterator();
        String files = "";
        while (a.hasNext()){
            files += a.next().getAll() + "\n";
        }
        return  getPull_request().getAll() +
                "changed_files: { " + files + "}\n";
    }

    public GitPullRequest getPull_request() { return pull_request; }

    public List<FileModel> getChanged_files() { return changed_files; }

}
