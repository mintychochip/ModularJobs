package net.aincraft.service;

import java.util.Collection;
import java.util.List;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.BoostType;
import org.bukkit.entity.Player;

public interface BoostService {

  interface BoostStore<T> {

  }

  <T> void addBoostSource(BoostSource boostSource, T object);
  <T> void addBoost(BoostType.Typed<T> type, T object);

  <T> void removeBoost(BoostType.Typed<T> type, T object);

  <T> List<Boost> getBoostsApplied(BoostType.Typed<T> type, T object);

  void clearAll();

  Collection<Boost> getBoosts(Player player);
}

