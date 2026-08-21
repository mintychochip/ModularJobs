package net.aincraft.payment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Accumulates damage by entity UUID and can expose each contributor's share.
 */
final class DamageContribution {

  private final Map<UUID, Double> contribution = new HashMap<>();

  private boolean dirty = true;
  private double sum;

  /**
   * Returns raw accumulated damage or the contributor's fraction of all damage.
   *
   * @param entity contributor to inspect
   * @param normalized whether to divide by the current total
   */
  double getContribution(Entity entity, boolean normalized) {
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

  /**
   * Resolves currently loaded contributors from their stored UUIDs.
   *
   * @return non-null entities that are still resolvable by Bukkit
   */
  @NotNull Collection<@NotNull Entity> getContributors() {
    return contribution.keySet().stream().map(Bukkit::getEntity).filter(Objects::nonNull)
        .toList();
  }

  /** Adds damage to the contributor's accumulated total. */
  void addContribution(Entity entity, double contribution) {
    dirty = true;
    this.contribution.merge(entity.getUniqueId(), contribution, Double::sum);
  }
}
