package net.aincraft.boost.conditions;

import com.google.common.base.Preconditions;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public record LiquidConditionImpl(Material liquid) implements Condition {

  public LiquidConditionImpl {
    Preconditions.checkArgument(liquid == Material.WATER || liquid == Material.LAVA,
        "Liquid material must be WATER or LAVA, got: " + liquid);
  }

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    if (liquid == Material.WATER) {
      return player.isInWater();
    }
    return player.isInLava();
  }

}
