package dev.mintychochip.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link MemoryStoreImpl} map-backed store contracts.
 */
class MemoryStoreImplTest {

  private MemoryStoreImpl<String, Integer> store;

  @BeforeEach
  void setUp() {
    store = new MemoryStoreImpl<>();
  }

  @Test
  void addThenGetAndContains() {
    assertFalse(store.contains("a"));
    store.add("a", 42);
    assertTrue(store.contains("a"));
    assertEquals(42, store.get("a"));
  }

  @Test
  void removeDropsEntry() {
    store.add("x", 1);
    store.remove("x");
    assertFalse(store.contains("x"));
    assertNull(store.get("x"));
  }

  @Test
  void addOverwritesExistingValue() {
    store.add("k", 1);
    store.add("k", 99);
    assertEquals(99, store.get("k"));
  }

  @Test
  void toStringReflectsContents() {
    store.add("hello", 7);
    String text = store.toString();
    assertTrue(text.contains("hello"), "toString should include key: " + text);
    assertTrue(text.contains("7"), "toString should include value: " + text);
  }
}
