package org.accula.api.code;

import lombok.Value;

@Value
public class CommitMarker {
    String owner;
    String repo;
    String sha;
    
    public String toString() {
        return owner + "/" + repo + "/" + sha;
    }
}
