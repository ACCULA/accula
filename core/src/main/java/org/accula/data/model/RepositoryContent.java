package org.accula.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = RepositoryContentDeserializer.class)
public class RepositoryContent {
    @JsonProperty @NonNull String filename;
    @JsonProperty @NonNull String path;
    @JsonProperty @NonNull String rawUrl;
    @JsonProperty @NonNull String linkToFile;
}
