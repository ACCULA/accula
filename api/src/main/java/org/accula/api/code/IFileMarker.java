package org.accula.api.code;


import org.accula.api.db.model.Commit;

/**
 * @author Anton Lamtev
 */
public interface IFileMarker {
    Commit getCommit();
    String getFilename();
}
