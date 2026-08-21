package dev.mintychochip;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a player's job, its progression rules, and its configured rewards.
 *
 * <p>A job is identified by its {@linkplain #key() key}; its curves and unlock maps define how
 * progression and perks are resolved.
 */
public interface Job extends Keyed {

  /** Returns the component displayed as this job's name. */
  @NotNull
  Component displayName();

  /** Returns the plain name. */
  String getPlainName();

  /** Returns the component describing this job. */
  @NotNull
  Component description();

  /** Returns the curve used to calculate experience thresholds. */
  @NotNull
  LevelingCurve levelingCurve();

  /**
   * Returns the configured payout curves keyed by payable type.
   *
   * <p>The returned map describes the job configuration and should be treated as read-only.
   */
  @NotNull
  Map<Key, PayableCurve> payableCurves();

  /** Max level. */
  int maxLevel();

  /** Upgrade level. */
  int upgradeLevel();

  /**
   * Returns perks grouped by the level at which they unlock.
   *
   * <p>The returned map describes the job configuration and should be treated as read-only.
   */
  @NotNull
  Map<Integer, List<String>> perkUnlocks();
}
