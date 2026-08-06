package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Checks whether the player has a potion effect identified by key/name
 * (resolved at evaluation time — no registry required at parse).
 */
public record PotionTypeConditionImpl(Key effectKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = Bukkit.getPlayer(context.playerId());
    if (player == null) {
      return false;
    }
    for (PotionEffect effect : player.getActivePotionEffects()) {
      if (matches(effect.getType())) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(PotionEffectType type) {
    Key key = type.getKey();
    return effectKey.equals(key)
        || effectKey.value().equalsIgnoreCase(key.value())
        || effectKey.asString().equalsIgnoreCase(key.asString());
  }
}
