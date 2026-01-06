package net.aincraft.upgrade.config;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JSON model for upgrade tree configuration.
 */
public record UpgradeTreeConfig(
    @NotNull String job,
    int skill_points_per_level,
    @NotNull String root,
    @NotNull Map<String, NodeConfig> nodes
) {

  public record NodeConfig(
      @NotNull String name,
      @Nullable String description,
      @NotNull String icon,
      int cost,
      @Nullable String type,
      @Nullable List<String> prerequisites,
      @Nullable List<String> exclusive,
      @Nullable List<String> children,
      @Nullable List<EffectConfig> effects,
      @Nullable PositionConfig position
  ) {
    /**
     * Default to MINOR if not specified.
     */
    public String resolvedType() {
      return type != null ? type : "minor";
    }
  }

  public record EffectConfig(
      @NotNull String type,
      @Nullable String target,
      @Nullable Double amount,
      @Nullable String ability,
      @Nullable String description,
      @Nullable String stat,
      @Nullable Integer value,
      @Nullable String unlock_type,
      @Nullable String unlock_key,
      @Nullable String permission
  ) {
  }

  public record PositionConfig(
      int x,
      int y
  ) {
  }
}
