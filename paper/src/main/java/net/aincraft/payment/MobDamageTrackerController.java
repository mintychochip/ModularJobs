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

/**
 * Records damage contributions onto a {@link MobDamageTrackerStore} as entities
 * are damaged. Tracking starts automatically on the first player-inflicted
 * damage and only updates already-tracked entities for non-player damage.
 */
final class MobDamageTrackerController implements Listener {

  private final MobDamageTrackerStore store;

  /** Creates the damage-tracking listener backed by the supplied store. */
  MobDamageTrackerController(MobDamageTrackerStore store) {
    this.store = store;
  }

  /**
   * On MONITOR priority, attributes {@code EntityDamageByEntityEvent} damage to
   * an entity: projectile damage is attributed to its shooter, and tameable
   * damage to its owning player. Non-player damage only affects entities that
   * are already being tracked.
   */
  @SuppressWarnings("UnstableApiUsage")
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDamageTrackedEntity(final EntityDamageByEntityEvent event) {
    Entity victim = event.getEntity();
    // Start tracking on the first player damage event.
    DamageSource damageSource = event.getDamageSource();
    Entity damager = damageSource.getCausingEntity();

    if (damager instanceof Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      if (!(shooter instanceof Entity entity)) {
        return;
      }
      damager = entity;
    }
    // Attribute tameable damage to its owner.
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
