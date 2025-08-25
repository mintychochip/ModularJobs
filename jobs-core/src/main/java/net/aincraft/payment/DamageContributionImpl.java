package net.aincraft.payment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.aincraft.payment.MobDamageTracker.DamageContribution;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

final class DamageContributionImpl implements DamageContribution {

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
