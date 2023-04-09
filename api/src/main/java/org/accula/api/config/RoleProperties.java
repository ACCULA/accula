package org.accula.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * @author Anton Lamtev
 */
@ConfigurationProperties("accula.role")
public record RoleProperties(Set<Long> root, Set<Long> admin) {
}
