package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProfessionCatalogTest {

  @Test
  void azothCatalogHasFifteenTracks() {
    assertEquals(15, ProfessionCatalog.tracks().size());
  }

  @Test
  void gatheringProcessingCraftingCounts() {
    assertEquals(5, ProfessionCatalog.tracksByCategory(ProfessionCategory.GATHERING).size());
    assertEquals(4, ProfessionCatalog.tracksByCategory(ProfessionCategory.PROCESSING).size());
    assertEquals(6, ProfessionCatalog.tracksByCategory(ProfessionCategory.CRAFTING).size());
  }

  @Test
  void resolvesCanonicalIds() {
    assertEquals("mining", ProfessionCatalog.resolve("mining").orElseThrow().id());
    assertEquals("weaponsmithing", ProfessionCatalog.resolve("weaponsmithing").orElseThrow().id());
    assertEquals("alchemy", ProfessionCatalog.resolve("alchemy").orElseThrow().id());
  }

  @Test
  void resolvesLegacyJobKeysToCanonical() {
    assertEquals("mining", ProfessionCatalog.resolve("miner").orElseThrow().id());
    assertEquals("woodcutting", ProfessionCatalog.resolve("lumberjack").orElseThrow().id());
    assertEquals("farming", ProfessionCatalog.resolve("farmer").orElseThrow().id());
    assertEquals("fishing", ProfessionCatalog.resolve("fisherman").orElseThrow().id());
    assertTrue(ProfessionCatalog.resolve("fisher").isEmpty());
    assertEquals("alchemy", ProfessionCatalog.resolve("alchemist").orElseThrow().id());
    assertEquals("weaponsmithing", ProfessionCatalog.resolve("blacksmith").orElseThrow().id());
  }

  @Test
  void resolvesNamespacedKeys() {
    assertEquals("mining", ProfessionCatalog.resolve("modularjobs:miner").orElseThrow().id());
  }

  @Test
  void storageKeysMapToExistingOrNewJobs() {
    assertEquals("miner", ProfessionCatalog.byId("mining").orElseThrow().storageKey());
    assertEquals("lumberjack", ProfessionCatalog.byId("woodcutting").orElseThrow().storageKey());
    assertEquals("blacksmith", ProfessionCatalog.byId("weaponsmithing").orElseThrow().storageKey());
    assertEquals("alchemist", ProfessionCatalog.byId("alchemy").orElseThrow().storageKey());
    assertEquals("herbalism", ProfessionCatalog.byId("herbalism").orElseThrow().storageKey());
  }

  @Test
  void nonCatalogJobsNotResolved() {
    assertTrue(ProfessionCatalog.resolve("builder").isEmpty());
    assertTrue(ProfessionCatalog.resolve("hunter").isEmpty());
    assertTrue(ProfessionCatalog.resolve("enchanter").isEmpty());
    assertFalse(ProfessionCatalog.isCanonicalTrack("builder"));
  }

  @Test
  void allCanonicalIdsAreUnique() {
    Set<String> ids = ProfessionCatalog.tracks().stream()
        .map(ProfessionDefinition::id)
        .collect(Collectors.toSet());
    assertEquals(15, ids.size());
  }
}
