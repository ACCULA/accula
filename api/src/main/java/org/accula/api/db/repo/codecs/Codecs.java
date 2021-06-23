package org.accula.api.db.repo.codecs;

import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.postgresql.extension.CodecRegistrar;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;

/**
 * @author Anton Lamtev
 */
public final class Codecs {
    private static final String PROJECT_STATE = "project_state_enum";
    private static final String USER_ROLE = "user_role_enum";

    private Codecs() {
    }

    public static CodecRegistrar enums() {
        return EnumCodec.builder()
            .withEnum(Codecs.PROJECT_STATE, Project.State.class)
            .withEnum(Codecs.USER_ROLE, User.Role.class)
            .build();
    }
}
