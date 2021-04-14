package org.accula.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

/**
 * @author Anton Lamtev
 */
public final class SerializationHelper {
    private SerializationHelper() {
    }

    @SneakyThrows
    public static String json(final Object o) {
        return new ObjectMapper().writeValueAsString(o);
    }
}
