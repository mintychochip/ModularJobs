package net.aincraft.payment;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import net.aincraft.payment.MobDamageTracker.DamageContribution;
import org.bukkit.entity.Entity;

final class MobDamageTrackerStoreImpl implements MobDamageTrackerStore {

  // Use Caffeine cache with TTL to handle entities that despawn/die in unloaded chunks
  // Entries expire after 10 minutes - entities shouldn't live longer than that in combat
  private final Cache<UUID, DamageContribution> damageContributions = Caffeine.newBuilder()
      .maximumSize(10_000)
      .expireAfterWrite(Duration.ofMinutes(10))
      .build();

  @Inject
  public MobDamageTrackerStoreImpl() {
  }

  @Override
  public DamageContribution getContribution(Entity entity,
      Supplier<DamageContribution> contributionSupplier) {
    DamageContribution existing = damageContributions.getIfPresent(entity.getUniqueId());
    if (existing != null) {
      return existing;
    }
    DamageContribution newContribution = contributionSupplier.get();
    damageContributions.put(entity.getUniqueId(), newContribution);
    return newContribution;
  }

  @Override
  public DamageContribution removeContribution(Entity entity) {
    DamageContribution contribution = damageContributions.getIfPresent(entity.getUniqueId());
    damageContributions.invalidate(entity.getUniqueId());
    return contribution;
  }

  @Override
  public boolean hasContribution(Entity entity) {
    return damageContributions.getIfPresent(entity.getUniqueId()) != null;
  }
}
