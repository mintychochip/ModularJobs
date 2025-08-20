package net.aincraft.util;

//import io.lumine.mythic.bukkit.MythicBukkit;
//import io.lumine.mythic.core.mobs.ActiveMob;

import net.aincraft.container.Context.EntityContext;
import net.aincraft.util.KeyResolver.KeyResolvingStrategy;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class EntityResolvingStrategyImpl implements KeyResolvingStrategy<EntityContext> {

  private static final String MYTHIC_MOBS = "MythicMobs";

  @Override
  public Key resolve(EntityContext object) {
    Entity entity = object.entity();
    Plugin mythic = Bukkit.getPluginManager().getPlugin(MYTHIC_MOBS);
    if (mythic != null && mythic.isEnabled()) {
//      Optional<ActiveMob> mobOptional = MythicBukkit.inst().getMobManager()
//          .getActiveMob(entity.getUniqueId());
//      if (mobOptional.isPresent()) {
//        ActiveMob mob = mobOptional.get();
//        String value = mob.getName();
//        return new NamespacedKey(mythic, value.toLowerCase(Locale.ENGLISH));
//      }
    }
    return entity.getType().key();
  }
}
