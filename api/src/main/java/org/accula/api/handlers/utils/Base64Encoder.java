package org.accula.api.handlers.utils;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class Base64Encoder {
    private final Base64.Encoder encoder = Base64.getEncoder();

    @Nullable
    public String encode(@Nullable final String data) {
        if (data == null) {
            return null;
        }
        return encoder.encodeToString(data.getBytes());
    }
}
