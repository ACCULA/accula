package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class GithubApiPull {
    @JsonProperty("html_url")
    private String htmlUrl;
    private Marker head;
    private Marker base;
    private GithubApiUser user;
    private Integer number;
    private String title;
    private State state;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;

    public boolean isValid() {
        return head.repo != null;
    }

    public enum State {
        ALL,
        OPEN,
        CLOSED,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Marker {
        private String label;
        private String ref;
        private GithubApiRepo repo;
        private String sha;

        public String getTreeUrl() {
            return String.format("%s/tree/%s", repo.getHtmlUrl(), URLEncoder.encode(ref, StandardCharsets.UTF_8));
        }
    }
}
