package org.accula.api.github.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Vadim Dyachkov
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor
public class GithubApiCollaborator {
    Long id;
    String login;
    GithubApiCollaboratorPermissions permissions;

    public boolean hasAdminPermissions() {
        return permissions != null && permissions.getAdmin() != null && permissions.getAdmin();
    }

    @Value
    @NoArgsConstructor(force = true, access = PRIVATE)
    @AllArgsConstructor
    public static class GithubApiCollaboratorPermissions {
        Boolean admin;
        Boolean push;
        Boolean pull;
    }
}
