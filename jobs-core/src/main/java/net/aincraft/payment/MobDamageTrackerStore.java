package net.aincraft.payment;

import java.util.function.Supplier;
import net.aincraft.payment.MobDamageTracker.DamageContribution;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
interface MobDamageTrackerStore {

  void registerTrackableEntity(Key key);

  boolean isTrackable(Entity entity);

  DamageContribution getContribution(Entity entity,
      Supplier<DamageContribution> contributionSupplier);

  DamageContribution removeContribution(Entity entity);

  boolean hasContribution(Entity entity);
}
