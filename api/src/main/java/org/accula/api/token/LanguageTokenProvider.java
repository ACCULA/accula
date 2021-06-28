package org.accula.api.token;

import net.jcip.annotations.ThreadSafe;
import org.accula.api.code.FileEntity;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
@ThreadSafe
public interface LanguageTokenProvider<Ref> {
    boolean supportsFile(FileEntity<Ref> file);

    Stream<List<Token<Ref>>> tokensByMethods(FileEntity<Ref> file);
}
