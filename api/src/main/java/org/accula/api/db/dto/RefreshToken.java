package org.accula.api.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
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
    private Long id;
    private Long userId;
    private String token;
    private Instant expirationDate;

    public static RefreshToken of(final Long userId,
                                  final String token,
                                  final Instant expirationDate) {
        return new RefreshToken(null, userId, token, expirationDate);
    }
}
