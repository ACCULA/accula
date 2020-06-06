package org.accula.api.db.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Builder
@Value
public class Pull {
    Long id;
    Long number;
    String title;
    boolean open;
    Instant createdAt;
    Instant updatedAt;
    Marker head;
    Marker base;
    GithubUser author;
    Project project;

    @Value
    public static class Marker {
        Commit commit;
        String branch;
        GithubRepo repo;
        GithubUser user;
    }
}
