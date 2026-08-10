package net.aincraft.profession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the {@code fish-catch-gates} section of config.yml into
 * {@link FishCatchGate}s.
 *
 * <p>Invalid entries (unknown/non-fish material, unknown profession,
 * non-positive/non-integer level) are skipped with a warning; a missing section
 * yields an empty list.
 */
public final class YamlFishCatchGateLoader {

  private static final String SECTION = "fish-catch-gates";
  private static final Set<Material> FISH_MATERIALS = Set.of(
      Material.COD,
      Material.SALMON,
      Material.TROPICAL_FISH,
      Material.PUFFERFISH);

  private final Logger logger;

  public YamlFishCatchGateLoader(@NotNull Logger logger) {
    this.logger = logger;
  }

  public @NotNull List<FishCatchGate> load(@NotNull ConfigurationSection config) {
    ConfigurationSection section = config.getConfigurationSection(SECTION);
    if (section == null) {
      return List.of();
    }
    Map<String, FishCatchGate> gates = new LinkedHashMap<>();
    for (String materialKey : section.getKeys(false)) {
      if (!section.isConfigurationSection(materialKey)) {
        logger.warning("fish-catch-gates: '" + materialKey
            + "' must be a configuration section — skipping");
        continue;
      }
      parseEntry(section.getConfigurationSection(materialKey), materialKey)
          .ifPresent(gate -> {
            if (gates.putIfAbsent(gate.itemKey(), gate) != null) {
              logger.warning("fish-catch-gates: duplicate item key '"
                  + gate.itemKey() + "' — keeping first entry");
            }
          });
    }
    return List.copyOf(gates.values());
  }

  private Optional<FishCatchGate> parseEntry(
      ConfigurationSection entry, String materialKey) {
    Material material = Material.matchMaterial(materialKey);
    if (material == null || !FISH_MATERIALS.contains(material)) {
      logger.warning("fish-catch-gates: unknown or non-fish material '"
          + materialKey + "' — skipping");
      return Optional.empty();
    }

    String professionId = entry.getString("profession");
    Optional<ProfessionDefinition> profession = ProfessionCatalog.resolve(professionId);
    if (profession.isEmpty()) {
      logger.warning("fish-catch-gates: '" + materialKey + "' has unknown profession '"
          + professionId + "' — skipping");
      return Optional.empty();
    }

    if (!entry.isInt("level")) {
      logger.warning("fish-catch-gates: '" + materialKey
          + "' level must be an int — skipping");
      return Optional.empty();
    }
    int level = entry.getInt("level");
    if (level <= 0) {
      logger.warning("fish-catch-gates: '" + materialKey
          + "' level must be > 0 — skipping");
      return Optional.empty();
    }

    return Optional.of(new FishCatchGate(
        materialKey.toLowerCase(Locale.ROOT),
        profession.orElseThrow().id().toLowerCase(Locale.ROOT),
        level));
  }
}
