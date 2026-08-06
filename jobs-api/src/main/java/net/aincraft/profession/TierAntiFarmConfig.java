package net.aincraft.profession;

import java.time.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * Config for tier-based profession XP anti-farm (AzothMC §8.2).
 *
 * @param window                 rolling window for diminishing-return repeats
 * @param cooldown               minimum time between XP awards for the same action key
 * @param diminishFactor         multiplier applied per prior attempt in the window (0–1)
 * @param maxRepeatsBeforeZero   after this many attempts in the window, XP multiplier is 0
 * @param belowLevelXpFactor     XP factor when resource tier is far below player level
 * @param levelsPerTier          player levels spanned by one resource tier (default 20 → T1=1–20)
 * @param belowLevelSlackLevels  how many levels above tier band before penalty applies
 */
public record TierAntiFarmConfig(
    @NotNull Duration window,
    @NotNull Duration cooldown,
    double diminishFactor,
    int maxRepeatsBeforeZero,
    double belowLevelXpFactor,
    int levelsPerTier,
    int belowLevelSlackLevels
) {

  public static TierAntiFarmConfig defaults() {
    return new TierAntiFarmConfig(
        Duration.ofMinutes(5),
        Duration.ofSeconds(2),
        0.5d,
        8,
        0.1d,
        20,
        20
    );
  }

  public TierAntiFarmConfig {
    if (diminishFactor < 0 || diminishFactor > 1) {
      throw new IllegalArgumentException("diminishFactor must be in [0,1]");
    }
    if (maxRepeatsBeforeZero < 1) {
      throw new IllegalArgumentException("maxRepeatsBeforeZero must be >= 1");
    }
    if (belowLevelXpFactor < 0 || belowLevelXpFactor > 1) {
      throw new IllegalArgumentException("belowLevelXpFactor must be in [0,1]");
    }
    if (levelsPerTier < 1) {
      throw new IllegalArgumentException("levelsPerTier must be >= 1");
    }
  }
}
