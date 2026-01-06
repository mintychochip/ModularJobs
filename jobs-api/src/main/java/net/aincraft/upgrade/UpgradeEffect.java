package net.aincraft.upgrade;

import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an effect granted by an upgrade node.
 * Currently a mock interface - implementations can be added later.
 */
public sealed interface UpgradeEffect permits
    UpgradeEffect.BoostEffect,
    UpgradeEffect.PassiveEffect,
    UpgradeEffect.StatEffect,
    UpgradeEffect.UnlockEffect {

  /**
   * Multiplier boost effect for XP or money.
   */
  record BoostEffect(@NotNull String target, @NotNull BigDecimal multiplier) implements UpgradeEffect {
    public static final String TARGET_XP = "xp";
    public static final String TARGET_MONEY = "money";
    public static final String TARGET_ALL = "all";
  }

  /**
   * Passive ability effect (auto-smelt, fortune, etc.).
   * Implementation is a placeholder for now.
   */
  record PassiveEffect(@NotNull String ability, @NotNull String description) implements UpgradeEffect {
  }

  /**
   * Stat modification effect (+max jobs, reduced cooldowns, etc.).
   */
  record StatEffect(@NotNull String stat, int value) implements UpgradeEffect {
    public static final String STAT_MAX_JOBS = "max_jobs";
    public static final String STAT_COOLDOWN_REDUCTION = "cooldown_reduction";
  }

  /**
   * Unlocks new content (tasks, features, etc.).
   */
  record UnlockEffect(@NotNull String unlockType, @NotNull String unlockKey) implements UpgradeEffect {
    public static final String UNLOCK_TASK = "task";
    public static final String UNLOCK_FEATURE = "feature";
  }
}
