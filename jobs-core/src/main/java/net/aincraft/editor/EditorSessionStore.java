package net.aincraft.editor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages editor sessions with automatic expiration.
 * Sessions are stored in a Caffeine cache and expire after the configured TTL.
 */
@Singleton
public final class EditorSessionStore {

    private final Cache<String, EditorSession> sessionCache;

    @Inject
    public EditorSessionStore(EditorConfig config) {
        this.sessionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(config.sessionTtlMinutes()))
            .build();
    }

    /**
     * Stores a new editor session.
     *
     * @param session The session to store
     */
    public void store(EditorSession session) {
        sessionCache.put(session.token(), session);
    }

    /**
     * Retrieves a session by its token.
     *
     * @param token The session token
     * @return Optional containing the session if found, empty otherwise
     */
    public Optional<EditorSession> get(String token) {
        return Optional.ofNullable(sessionCache.getIfPresent(token));
    }

    /**
     * Validates a session token against a player ID.
     *
     * @param token The session token to validate
     * @param playerId The player ID to check against
     * @return true if the session exists and belongs to the specified player
     */
    public boolean validate(String token, UUID playerId) {
        return get(token)
            .map(session -> session.playerId().equals(playerId))
            .orElse(false);
    }

    /**
     * Removes a session from the store.
     *
     * @param token The session token to remove
     */
    public void remove(String token) {
        sessionCache.invalidate(token);
    }
}
