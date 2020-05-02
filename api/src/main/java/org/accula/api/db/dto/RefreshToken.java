package org.accula.api.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Data
@AllArgsConstructor
public class RefreshToken {
    @Id
    @Nullable
    Long id;
    @NotNull
    Long userId;
    @NotNull
    String token;
    @NotNull
    Instant expirationDate;

    public static RefreshToken of(@NotNull final Long userId,
                                  @NotNull final String token,
                                  @NotNull final Instant expirationDate) {
        return new RefreshToken(null, userId, token, expirationDate);
    }
}
