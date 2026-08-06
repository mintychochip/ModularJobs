package net.aincraft.payment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.aincraft.payment.DamageContribution;
import org.bukkit.entity.Entity;

public final class MobDamageTrackerStore {

  private final Map<UUID, DamageContribution> damageContributions = new HashMap<>();

  public MobDamageTrackerStore() {
  }

  public DamageContribution getContribution(Entity entity,
      Supplier<DamageContribution> contributionSupplier) {
    return damageContributions.computeIfAbsent(entity.getUniqueId(),
        __ -> contributionSupplier.get());
  }

  public DamageContribution removeContribution(Entity entity) {
    return damageContributions.remove(entity.getUniqueId());
  }

  public boolean hasContribution(Entity entity) {
    return damageContributions.containsKey(entity.getUniqueId());
  }
}
