package org.accula.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.Set;

/**
 * @author Anton Lamtev
 */
@ConstructorBinding
@ConfigurationProperties("accula.role")
public record RoleProperties(Set<Long> root, Set<Long> admin) {
}
