package net.aincraft.profession;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * AzothMC §8.1 profession tracks plus legacy ModularJobs job-key aliases.
 *
 * <p>Canonical ids match the master design (mining, woodcutting, …). {@link #storageKey}
 * points at the jobs.yml / progression key ModularJobs actually uses today.
 */
public final class ProfessionCatalog {

  private static final List<ProfessionDefinition> TRACKS = List.of(
      // Gathering
      def("mining", "miner", ProfessionCategory.GATHERING, "Mining"),
      def("woodcutting", "lumberjack", ProfessionCategory.GATHERING, "Woodcutting"),
      def("herbalism", "herbalism", ProfessionCategory.GATHERING, "Herbalism"),
      def("farming", "farmer", ProfessionCategory.GATHERING, "Farming"),
      def("fishing", "fisherman", ProfessionCategory.GATHERING, "Fishing"),
      // Processing
      def("smelting", "smelting", ProfessionCategory.PROCESSING, "Smelting"),
      def("milling", "milling", ProfessionCategory.PROCESSING, "Milling"),
      def("tanning", "tanning", ProfessionCategory.PROCESSING, "Tanning"),
      def("refining", "refining", ProfessionCategory.PROCESSING, "Refining"),
      // Crafting
      def("cooking", "cooking", ProfessionCategory.CRAFTING, "Cooking"),
      def("alchemy", "alchemist", ProfessionCategory.CRAFTING, "Alchemy"),
      def("armorsmithing", "armorsmithing", ProfessionCategory.CRAFTING, "Armorsmithing"),
      def("weaponsmithing", "blacksmith", ProfessionCategory.CRAFTING, "Weaponsmithing"),
      def("tailoring", "tailoring", ProfessionCategory.CRAFTING, "Tailoring"),
      def("engineering", "engineering", ProfessionCategory.CRAFTING, "Engineering")
  );

  /** Legacy job keys → canonical profession id (not already a storageKey of a track). */
  private static final Map<String, String> LEGACY_ALIASES = Map.of(
      "lumberjack", "woodcutting",
      "miner", "mining",
      "farmer", "farming",
      "fisherman", "fishing",
      "alchemist", "alchemy",
      "blacksmith", "weaponsmithing"
  );

  private static final Map<String, ProfessionDefinition> BY_ID;
  private static final Map<String, ProfessionDefinition> BY_STORAGE;

  static {
    Map<String, ProfessionDefinition> byId = new LinkedHashMap<>();
    Map<String, ProfessionDefinition> byStorage = new LinkedHashMap<>();
    for (ProfessionDefinition d : TRACKS) {
      byId.put(d.id(), d);
      byStorage.put(d.storageKey(), d);
    }
    BY_ID = Collections.unmodifiableMap(byId);
    BY_STORAGE = Collections.unmodifiableMap(byStorage);
  }

  private ProfessionCatalog() {
  }

  private static ProfessionDefinition def(
      String id, String storageKey, ProfessionCategory category, String displayName) {
    return new ProfessionDefinition(id, storageKey, category, displayName);
  }

  /** All §8.1 tracks in catalog order. */
  public static @NotNull List<ProfessionDefinition> tracks() {
    return TRACKS;
  }

  public static @NotNull Collection<ProfessionDefinition> tracksByCategory(
      ProfessionCategory category) {
    return TRACKS.stream().filter(t -> t.category() == category).toList();
  }

  public static Optional<ProfessionDefinition> byId(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
  }

  public static Optional<ProfessionDefinition> byStorageKey(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_STORAGE.get(storageKey.toLowerCase(Locale.ROOT)));
  }

  /**
   * Resolve a canonical id, storage key, or legacy alias to a profession definition.
   */
  public static Optional<ProfessionDefinition> resolve(String idOrAlias) {
    if (idOrAlias == null || idOrAlias.isBlank()) {
      return Optional.empty();
    }
    String key = idOrAlias.toLowerCase(Locale.ROOT);
    // strip namespace if present (modularjobs:miner)
    int colon = key.indexOf(':');
    if (colon >= 0) {
      key = key.substring(colon + 1);
    }
    Optional<ProfessionDefinition> byId = byId(key);
    if (byId.isPresent()) {
      return byId;
    }
    Optional<ProfessionDefinition> byStorage = byStorageKey(key);
    if (byStorage.isPresent()) {
      return byStorage;
    }
    String aliased = LEGACY_ALIASES.get(key);
    if (aliased != null) {
      return byId(aliased);
    }
    return Optional.empty();
  }

  public static boolean isCanonicalTrack(String idOrAlias) {
    return resolve(idOrAlias).isPresent();
  }
}
