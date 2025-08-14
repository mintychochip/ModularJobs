package net.aincraft.api.service;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;

import org.bukkit.entity.Player;

public interface BoostService {

  interface BoostStore<T> {
    void addBoost(Boost boost, T object);
    void removeBoost(BoostType type, T object);
    Boost getBoost();
  }
  <T> void registerBoostProvider(BoostType.Typed<T> type, BoostStore<T> store);
  <T> void addBoost(BoostType.Typed<T> type, T object);

  <T> void removeBoost(BoostType.Typed<T> type, T object);

  <T> List<Boost> getBoostsApplied(BoostType.Typed<T> type, T object);

  void clearAll();

  Collection<Boost> getBoosts(Player player);
}

