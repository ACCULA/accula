package org.accula.api.code;

/**
 * @author Anton Lamtev
 */
public interface IFileSnippetMarker extends IFileMarker {
    int getFromLine();
    int getToLine();
}
