package net.aincraft.api.service;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;
import org.bukkit.entity.Player;

public interface BoostService {


  <T> void addBoost(BoostType.Typed<T> type, T object);

  <T> void removeBoost(BoostType.Typed<T> type, T object);

  <T> List<Boost> getBoostsApplied(BoostType.Typed<T> type, T object);

  void clearAll();

  Collection<Boost> getBoosts(Player player);
}

