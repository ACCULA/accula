package org.accula.api.github.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Vadim Dyachkov
 */
@Value
public class GithubApiCollaborator {
    Long id;
    String login;
    Permissions permissions;

    public boolean hasAdminPermissions() {
        return permissions != null && permissions.admin() != null && permissions.admin();
    }

    @Value
    @NoArgsConstructor(force = true, access = PRIVATE)
    @AllArgsConstructor
    public static class Permissions {
        Boolean admin;
        Boolean push;
        Boolean pull;
    }
}
