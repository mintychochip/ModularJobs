package dev.mintychochip.editor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Stores the Paper-local code-to-token handoff for REST editor sessions. */
public final class EditorSessionStore {

  private final Cache<String, EditorSession> sessionCache;

  /** Editor session store. */
  public EditorSessionStore(EditorConfig config) {
    this.sessionCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(config.sessionTtlMinutes()))
            .build();
  }

  /** Store. */
  public void store(EditorSession session) {
    sessionCache.put(session.sessionCode(), session);
  }

  /** Get. */
  public Optional<EditorSession> get(String sessionCode) {
    EditorSession session = sessionCache.getIfPresent(sessionCode);
    if (session == null) {
      return Optional.empty();
    }
    if (session.isExpired(Instant.now())) {
      sessionCache.invalidate(sessionCode);
      return Optional.empty();
    }
    return Optional.of(session);
  }

  /** Returns the owned. */
  public Optional<EditorSession> getOwned(String sessionCode, UUID playerId) {
    return get(sessionCode).filter(session -> session.playerId().equals(playerId));
  }

  /** Remove. */
  public void remove(String sessionCode) {
    sessionCache.invalidate(sessionCode);
  }
}
