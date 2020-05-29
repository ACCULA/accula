package org.accula.api.code;

import lombok.Value;
import org.accula.api.db.model.Commit;

/**
 * @author Anton Lamtev
 */
@Value
public class FileMarker implements IFileMarker {
    Commit commit;
    String filename;
}
