package org.accula.api.db.repo.codecs;

import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.postgresql.extension.CodecRegistrar;
import org.accula.api.db.model.CodeLanguage;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;

/**
 * @author Anton Lamtev
 */
public final class Codecs {
    private static final String PROJECT_STATE = "project_state_enum";
    private static final String USER_ROLE = "user_role_enum";
    private static final String CODE_LANGUAGE = "code_language_enum";

    private Codecs() {
    }

    public static CodecRegistrar enums() {
        return EnumCodec.builder()
            .withEnum(PROJECT_STATE, Project.State.class)
            .withEnum(USER_ROLE, User.Role.class)
            .withEnum(CODE_LANGUAGE, CodeLanguage.class)
            .build();
    }
}
