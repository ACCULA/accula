package org.accula.api.handler.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
public class CreateProjectDto implements InputDto {
    String githubRepoUrl;
}
