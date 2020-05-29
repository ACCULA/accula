package org.accula.api.code;

import lombok.Value;
import org.accula.api.db.model.Commit;

/**
 * @author Anton Lamtev
 */
@Value
public class FileSnippetMarker implements IFileSnippetMarker {
    Commit commit;
    String filename;
    int fromLine;
    int toLine;
}
