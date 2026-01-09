package net.aincraft.upgrade;

import net.aincraft.container.BoostSource;
import net.kyori.adventure.key.Key;

import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving boost sources from unlocked upgrade nodes.
 * Upgrade boosts now use the same BoostSource/BoostContext composition API as item boosts.
 */
public interface UpgradeBoostDataService {

    /**
     * Get boost sources from unlocked upgrade nodes.
     * Each returned BoostSource can contain rules and conditions that are evaluated
     * in the same way as item boosts, using the full composition API.
     *
     * @param playerId Player UUID
     * @param jobKey Job to get boosts for
     * @return List of boost sources from unlocked upgrade nodes
     */
    List<BoostSource> getBoostSources(UUID playerId, Key jobKey);
}
