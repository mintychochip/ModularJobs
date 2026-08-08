package net.aincraft.profession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the {@code block-break-gates} section of config.yml into {@link BlockBreakGate}s.
 * Invalid entries (unknown material/profession, non-positive level) are skipped
 * with a warning; a missing section yields an empty list.
 */
public final class YamlBlockBreakGateLoader {

  private static final String SECTION = "block-break-gates";

  private final Logger logger;

  public YamlBlockBreakGateLoader(@NotNull Logger logger) {
    this.logger = logger;
  }

  public @NotNull List<BlockBreakGate> load(@NotNull ConfigurationSection config) {
    ConfigurationSection section = config.getConfigurationSection(SECTION);
    if (section == null) {
      return List.of();
    }
    List<BlockBreakGate> gates = new ArrayList<>();
    for (String materialKey : section.getKeys(false)) {
      if (section.isConfigurationSection(materialKey)) {
        parseEntry(section.getConfigurationSection(materialKey), materialKey).ifPresent(gates::add);
      }
    }
    return gates;
  }

  private java.util.Optional<BlockBreakGate> parseEntry(
      ConfigurationSection entry, String materialKey) {
    Material material = Material.matchMaterial(materialKey);
    if (material == null || material == Material.AIR) {
      logger.warning("block-break-gates: unknown material '" + materialKey + "' — skipping");
      return java.util.Optional.empty();
    }
    String professionId = entry.getString("profession");
    if (professionId == null
        || !ProfessionCatalog.resolve(professionId).isPresent()) {
      logger.warning("block-break-gates: '" + materialKey + "' has unknown profession '"
          + professionId + "' — skipping");
      return java.util.Optional.empty();
    }
    if (!entry.isInt("level")) {
      logger.warning("block-break-gates: '" + materialKey + "' level must be an int — skipping");
      return java.util.Optional.empty();
    }
    int level = entry.getInt("level");
    if (level <= 0) {
      logger.warning("block-break-gates: '" + materialKey + "' level must be > 0 — skipping");
      return java.util.Optional.empty();
    }
    String canonical = ProfessionCatalog.resolve(professionId).orElseThrow()
        .id().toLowerCase(Locale.ROOT);
    return java.util.Optional.of(
        new BlockBreakGate(materialKey.toLowerCase(Locale.ROOT), canonical, level));
  }
}
