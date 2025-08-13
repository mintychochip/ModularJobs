package net.aincraft.service;

import net.aincraft.api.service.MobDamageTracker;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

public final class MobDamageTrackerImpl implements MobDamageTracker {

  private final MobDamageTrackerStore store;

  public MobDamageTrackerImpl(MobDamageTrackerStore store, Plugin plugin) {
    this.store = store;
    Bukkit.getPluginManager().registerEvents(new MobDamageTrackerListener(store), plugin);
  }

  @Override
  public void registerEntity(Key key) {
    store.registerTrackableEntity(key);
  }

  @Override
  public DamageContribution endTracking(Entity entity) throws IllegalArgumentException {
    return store.removeContribution(entity);
  }

  @Override
  public boolean isTracking(Entity entity) {
    return store.hasContribution(entity);
  }


  private record MobDamageTrackerListener(MobDamageTrackerStore store) implements Listener {

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onDamageTrackedEntity(final EntityDamageByEntityEvent event) {
      Entity victim = event.getEntity();
      if (!store.hasContribution(victim)) {
        return;
      }
      DamageSource damageSource = event.getDamageSource();
      Entity damager = damageSource.getCausingEntity();

      if (damager instanceof Projectile projectile) {
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Entity entity)) {
          return;
        }
        damager = entity;
      }
      DamageContribution contribution = store.getContribution(victim, DamageContributionImpl::new);
      contribution.addContribution(damager, event.getFinalDamage());
    }
  }
}
