package net.aincraft.payment;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.aincraft.service.MobDamageTracker;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

final class MobDamageTrackerImpl implements MobDamageTracker {

  private final MobDamageTrackerStore store;

  @Inject
  public MobDamageTrackerImpl(MobDamageTrackerStore store) {
    this.store = store;
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


  private static final class DamageContributionImpl implements DamageContribution {

    private final Map<UUID, Double> contribution = new HashMap<>();

    private boolean dirty = true;
    private double sum;

    @Override
    public double getContribution(Entity entity, boolean normalized) {
      UUID uniqueId = entity.getUniqueId();
      double raw = contribution.getOrDefault(uniqueId, 0.0);

      if (!normalized) {
        return raw;
      }

      if (dirty) {
        sum = contribution.values().stream().mapToDouble(Double::doubleValue).sum();
        dirty = false;
      }

      if (sum == 0.0) {
        return 0.0;
      }

      return raw / sum;
    }

    @Override
    public @NotNull Collection<@NotNull Entity> getContributors() {
      return contribution.keySet().stream().map(Bukkit::getEntity).filter(Objects::nonNull)
          .toList();
    }

    @Override
    public void addContribution(Entity entity, double contribution) {
      dirty = true;
      this.contribution.merge(entity.getUniqueId(), contribution, Double::sum);
    }
  }

  static final class MobDamageTrackerController implements Listener {

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
}
