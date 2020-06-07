package org.accula.api.handlers.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class CreateProjectRequestBody {
    String githubRepoUrl;
}
