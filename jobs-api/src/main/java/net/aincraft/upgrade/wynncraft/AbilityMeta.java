package net.aincraft.upgrade.wynncraft;

import java.util.List;
import java.util.Optional;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata for ability nodes in a Wynncraft-style skill tree.
 * Ability nodes are playable upgrades that grant effects.
 *
 * @param name          display name
 * @param icon          icon configuration
 * @param cost          skill point cost to unlock
 * @param description   list of description lines (supports multi-line lore)
 * @param prerequisites list of node IDs that must be unlocked first
 * @param exclusiveWith list of node IDs that become locked if this is unlocked
 * @param effects       list of effects granted by this ability
 * @param required      whether this node is required (always unlocked)
 * @param major         whether this is a major/specialization node
 * @param perkId        optional perk identifier for leveled perks (e.g., "crit_chance")
 * @param level         optional perk level (1, 2, 3...)
 */
public record AbilityMeta(
    @NotNull String name,
    @NotNull IconConfig icon,
    int cost,
    @NotNull List<String> description,
    @NotNull List<String> prerequisites,
    @NotNull List<String> exclusiveWith,
    @NotNull List<EffectConfig> effects,
    boolean required,
    boolean major,
    @Nullable List<Position> pathFromParent,
    @Nullable String perkId,
    @Nullable Integer level
) implements LayoutItemMeta {
  /**
   * Combined description as a single string with newlines.
   */
  public String combinedDescription() {
    return String.join("\n", description);
  }

  /**
   * Configuration for upgrade effects.
   *
   * @param type       the effect type (boost, passive, stat, unlock, permission, ruled_boost)
   * @param target     target for boost effects (e.g., "xp", "money")
   * @param amount     multiplier amount for boost effects
   * @param ability    ability name for passive effects
   * @param description effect description
   * @param stat       stat name for stat effects
   * @param value      stat value for stat effects
   * @param unlockType unlock type for unlock effects
   * @param unlockKey  unlock key for unlock effects
   * @param permission permission string for permission effects
   * @param policy     policy config for ruled_boost effects
   * @param rules      list of rules for ruled_boost effects
   */
  public record EffectConfig(
      @NotNull String type,
      @Nullable String target,
      @Nullable Double amount,
      @Nullable String ability,
      @Nullable String description,
      @Nullable String stat,
      @Nullable Integer value,
      @Nullable String unlockType,
      @Nullable String unlockKey,
      @Nullable String permission,
      @Nullable PolicyConfig policy,
      @Nullable java.util.List<RuleConfig> rules
  ) {
  }

  /**
   * Policy configuration for ruled_boost effects.
   */
  public record PolicyConfig(
      @NotNull String type,
      @Nullable Integer topK
  ) {
  }

  /**
   * Rule configuration for ruled_boost effects.
   */
  public record RuleConfig(
      int priority,
      @NotNull Object conditions,
      @NotNull BoostConfig boost
  ) {
  }

  /**
   * Boost configuration for ruled_boost effects.
   */
  public record BoostConfig(
      @NotNull String type,
      @NotNull Double amount
  ) {
  }
}
