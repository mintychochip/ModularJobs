package net.aincraft.upgrade;

import java.util.List;

public interface UserUpgradeRepository {
  List<String> getAllUpgradedNodes(String playerId);
  void addUpgrade(String playerId, String nodeKey);
  void removeUpgrade(String playerId, String nodeKey);
}
