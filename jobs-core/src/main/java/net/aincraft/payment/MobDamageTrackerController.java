package net.aincraft.payment;

import com.google.inject.Inject;
import net.aincraft.payment.MobDamageTracker.DamageContribution;
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

  @Inject
  MobDamageTrackerController(MobDamageTrackerStore store) {
    this.store = store;
  }

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
