package net.aincraft.editor;

import java.time.Instant;
import java.util.UUID;

/**
 * Paper-local handoff for a REST editor session.
 *
 * @param sessionCode public REST session identifier
 * @param token secret REST session token
 * @param playerId player who created and may apply the session
 * @param createdAt local creation time
 * @param expiresAt REST session expiry time
 */
public record EditorSession(
    String sessionCode,
    String token,
    UUID playerId,
    Instant createdAt,
    Instant expiresAt
) {
  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }
}
