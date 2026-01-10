package net.aincraft.payment;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.aincraft.payment.MobDamageTracker.DamageContribution;
import org.bukkit.entity.Entity;

final class MobDamageTrackerStoreImpl implements MobDamageTrackerStore {

  private final Map<UUID, DamageContribution> damageContributions = new HashMap<>();

  @Inject
  public MobDamageTrackerStoreImpl() {
  }

  @Override
  public DamageContribution getContribution(Entity entity,
      Supplier<DamageContribution> contributionSupplier) {
    return damageContributions.computeIfAbsent(entity.getUniqueId(),
        __ -> contributionSupplier.get());
  }

  @Override
  public DamageContribution removeContribution(Entity entity) {
    return damageContributions.remove(entity.getUniqueId());
  }

  @Override
  public boolean hasContribution(Entity entity) {
    return damageContributions.containsKey(entity.getUniqueId());
  }
}
