package net.aincraft.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType.Typed;
import net.aincraft.api.container.Store;
import net.aincraft.api.service.BoostService;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

public class BoostServiceImpl implements BoostService {

  private final Map<Key, Store<UUID, UUID>> stores = new HashMap<>();
  @Override
  public <T> void addBoost(Typed<T> type, T object) {
    Store<UUID, UUID> store = stores.get(type.key());
//    store.contains()
  }

  @Override
  public <T> void removeBoost(Typed<T> type, T object) {

  }

  @Override
  public <T> List<Boost> getBoostsApplied(Typed<T> type, T object) {
    return List.of();
  }

  @Override
  public void clearAll() {

  }

  @Override
  public Collection<Boost> getBoosts(Player player) {
    return List.of();
  }
}
