package dev.mintychochip.upgrade.config;

import com.google.gson.annotations.SerializedName;
import dev.mintychochip.boost.config.BoostSourceConfig.ConditionConfig;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** JSON model for upgrade tree configuration. */
public record UpgradeTreeConfig(
    @NotNull String job,
    @Nullable String description,
    @SerializedName("skill_points_per_level") int skillPointsPerLevel,
    @NotNull String root,
    @NotNull Map<String, NodeConfig> nodes) {

  /** Node config. */
  public record NodeConfig(
      @NotNull String name,
      @Nullable String description,
      @NotNull String icon,
      int cost,
      @Nullable List<String> prerequisites,
      @Nullable List<String> exclusive,
      @Nullable List<String> children,
      @Nullable List<EffectConfig> effects,
      @Nullable PositionConfig position,
      @Nullable String perkId,
      @Nullable Integer level) {}

  /**
   * Effect configuration for upgrade nodes. Supports both simple boosts and ruled boosts with
   * conditions.
   */
  public record EffectConfig(
      @NotNull String type,
      @Nullable String target,
      @Nullable Double amount,
      @Nullable String ability,
      @Nullable String description,
      @Nullable String stat,
      @Nullable Integer value,
      @SerializedName("unlock_type") @Nullable String unlockType,
      @SerializedName("unlock_key") @Nullable String unlockKey,
      @Nullable String permission,
      @Nullable List<String> permissions,
      // For ruled_boost type: use the same structure as boost_sources
      @Nullable List<RuleConfig> rules) {}

  /** Rule configuration for ruled boost effects. Same structure as boost source rules. */
  public record RuleConfig(
      int priority, @NotNull ConditionConfig conditions, @NotNull BoostConfig boost) {}

  /** Boost amount configuration. */
  public record BoostConfig(@NotNull String type, double amount) {}

  /** Position config. */
  public record PositionConfig(int x, int y) {}
}
