package net.aincraft.upgrade;

import java.util.List;
import net.aincraft.repository.ConnectionSource;

public class UserUpgradeRepositoryImpl implements UserUpgradeRepository {

  private final ConnectionSource connectionSource;

  public UserUpgradeRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public List<String> getAllUpgradedNodes(String playerId) {
    return List.of();
  }

  @Override
  public void addUpgrade(String playerId, String nodeKey) {

  }

  @Override
  public void removeUpgrade(String playerId, String nodeKey) {

  }
}
