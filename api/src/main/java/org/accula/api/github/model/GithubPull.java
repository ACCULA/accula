package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Data
@AllArgsConstructor
public final class GithubPull {
    @JsonProperty("html_url")
    private String htmlUrl;
    private Marker head;
    private Marker base;
    private GithubUser user;
    private Integer number;
    private String title;
    private State state;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;

    public enum State {
        OPEN,
        CLOSED,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }

    @Data
    @AllArgsConstructor
    public static final class Marker {
        private String label;
        private String ref;
        private GithubRepo repo;
        private String sha;

        public String getTreeUrl() {
            return String.format("%s/tree/%s", repo.getHtmlUrl(), URLEncoder.encode(ref, StandardCharsets.UTF_8));
        }
    }
}
