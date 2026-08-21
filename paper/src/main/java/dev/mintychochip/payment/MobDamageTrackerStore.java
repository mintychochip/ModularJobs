package dev.mintychochip.payment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Entity;

/**
 * In-memory, entity-UUID-keyed store of active {@link DamageContribution} tracking.
 */
public final class MobDamageTrackerStore {

  private final Map<UUID, DamageContribution> damageContributions = new HashMap<>();

  /** Creates an empty tracking store. */
  public MobDamageTrackerStore() {
  }

  /**
   * Returns the existing contribution for {@code entity} or creates (and stores) one via
   * {@code contributionSupplier}.
   */
  public DamageContribution getContribution(Entity entity,
      Supplier<DamageContribution> contributionSupplier) {
    return damageContributions.computeIfAbsent(entity.getUniqueId(),
        ignoredKey -> contributionSupplier.get());
  }

  /** Removes and returns the contribution for {@code entity}, or null when not tracked. */
  public DamageContribution removeContribution(Entity entity) {
    return damageContributions.remove(entity.getUniqueId());
  }

  /**
   * Reports whether {@code entity} has an active contribution being tracked.
   *
   * @return true when {@code entity} has an active contribution being tracked
   */
  public boolean hasContribution(Entity entity) {
    return damageContributions.containsKey(entity.getUniqueId());
  }
}
