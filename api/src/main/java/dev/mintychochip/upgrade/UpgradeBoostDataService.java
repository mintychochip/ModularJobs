package dev.mintychochip.upgrade;

import dev.mintychochip.container.BoostSource;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Service for retrieving boost sources from unlocked upgrade nodes. Upgrade boosts now use the same
 * BoostSource/BoostContext composition API as item boosts.
 */
@FunctionalInterface
public interface UpgradeBoostDataService {

  /**
   * Get boost sources from unlocked upgrade nodes. When a v2 skill tree state and matching {@link
   * SkillTree} exist, sources are derived from the active effects of each owned node. Otherwise the
   * legacy UpgradeTree path is used. Each returned BoostSource can contain rules and conditions
   * that are evaluated in the same way as item boosts, using the full composition API.
   *
   * @param playerId Player UUID
   * @param jobKey Job to get boosts for
   * @return List of boost sources from unlocked upgrade nodes
   */
  @NotNull
  List<BoostSource> getBoostSources(@NotNull UUID playerId, @NotNull Key jobKey);
}
