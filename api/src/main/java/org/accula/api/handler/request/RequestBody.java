package org.accula.api.handler.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

/**
 * @author Anton Lamtev
 */
@JsonInclude(ALWAYS)
@JsonAutoDetect(fieldVisibility = ANY)
public interface RequestBody {
}
