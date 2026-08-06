package net.aincraft.payment;

import net.aincraft.payment.DamageContribution;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

final class MobDamageTrackerController implements Listener {

  private final MobDamageTrackerStore store;

  MobDamageTrackerController(MobDamageTrackerStore store) {
    this.store = store;
  }

  @SuppressWarnings("UnstableApiUsage")
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDamageTrackedEntity(final EntityDamageByEntityEvent event) {
    Entity victim = event.getEntity();
    // Jobs Reborn MonsterDamage / multi-contrib: start tracking on first player damage
    DamageSource damageSource = event.getDamageSource();
    Entity damager = damageSource.getCausingEntity();

    if (damager instanceof Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      if (!(shooter instanceof Entity entity)) {
        return;
      }
      damager = entity;
    }
    // Attribute pet damage to owner (Jobs Reborn TameablesPayout spirit)
    if (damager instanceof org.bukkit.entity.Tameable tameable
        && tameable.getOwner() instanceof org.bukkit.entity.Player owner) {
      damager = owner;
    }
    if (!(damager instanceof org.bukkit.entity.Player)) {
      // Environmental / mob damage: only update if already tracking
      if (!store.hasContribution(victim)) {
        return;
      }
    }
    DamageContribution contribution = store.getContribution(victim, DamageContribution::new);
    contribution.addContribution(damager, event.getFinalDamage());
  }
}
