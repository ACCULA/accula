package org.accula.data.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PullRequestDeserializer.class)
public class PullRequest {
    @JsonProperty @NonNull Long id;
    @JsonProperty @NonNull Integer number;
    @JsonProperty @NonNull String title;
    @JsonProperty @NonNull Instant created;
    @JsonProperty @NonNull String userName;
    @JsonProperty String link;
}
