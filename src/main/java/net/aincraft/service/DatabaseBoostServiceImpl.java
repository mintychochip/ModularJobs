package net.aincraft.service;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType.Typed;
import net.aincraft.api.service.BoostService;
import net.aincraft.database.ConnectionSource;
import org.bukkit.entity.Player;

public class DatabaseBoostServiceImpl implements BoostService {

  private final ConnectionSource connectionSource;

  public DatabaseBoostServiceImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public <T> void addBoost(Typed<T> type, T object) {
    connectionSource.
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
