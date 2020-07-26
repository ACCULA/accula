package org.accula.api.code.git;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class File {
    public String objectId;
    @EqualsAndHashCode.Exclude
    public String name;
}
