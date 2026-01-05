package net.aincraft.editor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a web editor session.
 * Sessions are temporary and expire after a configured TTL.
 *
 * @param token Random UUID token for verification
 * @param playerId The player who created this session
 * @param createdAt When the session was created
 * @param bytebinCode The bytebin paste code containing the job data
 */
public record EditorSession(
    String token,
    UUID playerId,
    Instant createdAt,
    String bytebinCode
) {
}
