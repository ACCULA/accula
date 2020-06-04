package org.accula.api.db.model;

import io.micrometer.core.lang.Nullable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class User {
    Long id;
    Long ghId;
    String ghLogin;
    @Nullable
    String ghName;
    String ghAvatar;
    @Nullable
    String ghAccessToken;
}
