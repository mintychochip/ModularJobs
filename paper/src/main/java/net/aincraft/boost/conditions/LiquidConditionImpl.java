package net.aincraft.boost.conditions;

import com.google.common.base.Preconditions;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Checks if the player is in a specific liquid (water or lava).
 * Material is stored as a key/name string and resolved at evaluation.
 */
public record LiquidConditionImpl(String materialKey) implements Condition {

  public LiquidConditionImpl {
    Preconditions.checkArgument(materialKey != null && !materialKey.isBlank(),
        "Liquid material key must be non-blank");
  }

  @Override
  public boolean applies(BoostContext context) {
    Player player = Bukkit.getPlayer(context.playerId());
    if (player == null) {
      return false;
    }
    Material liquid = Material.matchMaterial(materialKey);
    if (liquid == null) {
      // bare names / keys without registry prefix
      String bare = materialKey.contains(":")
          ? materialKey.substring(materialKey.indexOf(':') + 1)
          : materialKey;
      liquid = Material.matchMaterial(bare);
    }
    if (liquid == Material.WATER || isWaterName(materialKey)) {
      return player.isInWater();
    }
    if (liquid == Material.LAVA || isLavaName(materialKey)) {
      return player.isInLava();
    }
    return false;
  }

  private static boolean isWaterName(String key) {
    String v = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
    return "water".equalsIgnoreCase(v);
  }

  private static boolean isLavaName(String key) {
    String v = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
    return "lava".equalsIgnoreCase(v);
  }
}
