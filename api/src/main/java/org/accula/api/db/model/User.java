package org.accula.api.db.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class User {
    Long id;
    Long ghId;
    String ghLogin;
    String ghName;
    String ghAvatar;
    String ghAccessToken;
}
