package net.aincraft.upgrade.wynncraft;

import java.util.List;
import java.util.Optional;
import net.aincraft.upgrade.UpgradeEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata for ability nodes in a Wynncraft-style skill tree.
 * Ability nodes are playable upgrades that grant effects.
 *
 * @param name          display name
 * @param icons         icon configurations for locked and unlocked states
 * @param cost          skill point cost to unlock
 * @param description     list of description lines (supports multi-line lore)
 * @param prerequisites   list of node IDs that must ALL be unlocked first (AND logic)
 * @param prerequisitesOr list of node IDs where at least ONE must be unlocked (OR logic)
 * @param exclusiveWith   list of node IDs that become locked if this is unlocked
 * @param effects       list of effects granted by this ability
 * @param required      whether this node is required (always unlocked)
 * @param major         whether this is a major/specialization node
 * @param perkId            optional perk identifier for leveled perks (e.g., "crit_chance")
 * @param level             optional perk level (1, 2, 3...)
 * @param maxLevel          optional max level for in-place upgrades (>1 enables upgrades)
 * @param levelCosts        optional per-level costs [lvl1, lvl2, lvl3] (null = use cost for all)
 * @param levelDescriptions optional per-level descriptions (null = use base description)
 * @param levelEffects      optional per-level effects (null = use base effects)
 * @param levelIcons        optional per-level icons (null = use base icon)
 */
public record AbilityMeta(
    @NotNull String name,
    @NotNull AbilityIcons icons,
    int cost,
    @NotNull List<String> description,
    @NotNull List<String> prerequisites,
    @NotNull List<String> prerequisitesOr,
    @NotNull List<String> exclusiveWith,
    @NotNull List<EffectConfig> effects,
    boolean required,
    boolean major,
    @Nullable String perkId,
    @Nullable Integer level,
    @Nullable Integer maxLevel,
    @Nullable List<Integer> levelCosts,
    @Nullable List<String> levelDescriptions,
    @Nullable List<List<EffectConfig>> levelEffects,
    @Nullable List<IconConfig> levelIcons
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
   * @param permission permission string for permission effects (deprecated, use permissions)
   * @param permissions list of permissions for permission effects
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
      @Nullable java.util.List<String> permissions,
      @Nullable java.util.List<RuleConfig> rules
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

  /**
   * Icon configurations for ability node states.
   *
   * @param locked   icon shown when node is locked (not yet unlocked)
   * @param unlocked icon shown when node is unlocked
   */
  public record AbilityIcons(
      @NotNull IconConfig locked,
      @NotNull IconConfig unlocked
  ) {
  }
}
