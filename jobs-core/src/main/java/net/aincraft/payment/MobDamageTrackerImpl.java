package net.aincraft.payment;

import com.google.inject.Inject;
import net.aincraft.service.MobDamageTracker;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;

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


}
