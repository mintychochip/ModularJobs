package net.aincraft.upgrade;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.container.BoostSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an effect granted by an upgrade node.
 */
public sealed interface UpgradeEffect permits
    UpgradeEffect.BoostEffect,
    UpgradeEffect.RuledBoostEffect,
    UpgradeEffect.PermissionEffect {

  /**
   * Simple boost effect for XP or money (legacy format).
   * For complex conditions, use {@link RuledBoostEffect}.
   */
  record BoostEffect(@NotNull String target, @NotNull BigDecimal multiplier) implements UpgradeEffect {
    public static final String TARGET_XP = "xp";
    public static final String TARGET_MONEY = "money";
    public static final String TARGET_ALL = "all";
  }

  /**
   * Boost effect that uses the full BoostSource composition API with rules/conditions.
   * Supports same condition types as item boosts (weather, biome, potion effects, etc).
   *
   * @param target      target payable type (xp/money/all)
   * @param boostSource the boost source with rules and conditions
   */
  record RuledBoostEffect(
      @NotNull String target,
      @NotNull BoostSource boostSource
  ) implements UpgradeEffect {
  }

  /**
   * Grants a temporary permission via Bukkit PermissionAttachment.
   * Permission is revoked on logout or tree reset.
   */
  record PermissionEffect(@NotNull String permission) implements UpgradeEffect {
  }
}
