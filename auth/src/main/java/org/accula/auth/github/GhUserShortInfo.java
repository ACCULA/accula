package org.accula.auth.github;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GhUserShortInfo {
    public final long id;
    public final String login;
    public final String name;
}
