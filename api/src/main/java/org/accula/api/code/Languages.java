package org.accula.api.code;

import org.accula.api.db.model.CodeLanguage;

import java.util.Collection;
import java.util.EnumSet;

/**
 * @author Anton Lamtev
 */
public final class Languages {
    private Languages() {
    }

    public static FileFilter filter(final Collection<CodeLanguage> languages) {
        final var languageSet = EnumSet.copyOf(languages);
        final var java = languageSet.contains(CodeLanguage.JAVA);
        final var kotlin = languageSet.contains(CodeLanguage.KOTLIN);
        if (java && kotlin) {
            return JvmFileFilter.MAIN;
        } else if (java) {
            return JvmFileFilter.JAVA_MAIN;
        }
        return JvmFileFilter.KOTLIN_MAIN;
    }
}
