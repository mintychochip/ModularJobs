package net.aincraft.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditorSessionStoreTest {

  @Test
  void looksUpSessionsByCodeAndEnforcesOwner() {
    UUID owner = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    Instant now = Instant.parse("2030-01-01T00:00:00Z");
    EditorSession session = new EditorSession(
        "code-a",
        "token-a",
        owner,
        now,
        now.plusSeconds(3600));
    EditorSessionStore store = new EditorSessionStore(EditorConfig.defaults());

    store.store(session);

    assertEquals(session, store.get("code-a").orElseThrow());
    assertEquals(session, store.getOwned("code-a", owner).orElseThrow());
    assertTrue(store.getOwned("code-a", other).isEmpty());
  }

  @Test
  void removesSessionByCode() {
    UUID owner = UUID.randomUUID();
    Instant now = Instant.parse("2030-01-01T00:00:00Z");
    EditorSession session = new EditorSession(
        "code-a",
        "token-a",
        owner,
        now,
        now.plusSeconds(3600));
    EditorSessionStore store = new EditorSessionStore(EditorConfig.defaults());
    store.store(session);

    store.remove("code-a");

    assertTrue(store.get("code-a").isEmpty());
  }

  @Test
  void rejectsExpiredOwnedSession() {
    UUID owner = UUID.randomUUID();
    Instant now = Instant.parse("2020-01-01T00:00:00Z");
    EditorSession session = new EditorSession(
        "expired",
        "token-a",
        owner,
        now.minusSeconds(3600),
        now.minusSeconds(1));
    EditorSessionStore store = new EditorSessionStore(EditorConfig.defaults());
    store.store(session);

    assertTrue(store.getOwned("expired", owner).isEmpty());
    assertTrue(store.get("expired").isEmpty());
  }
}
