package net.aincraft.upgrade;

import java.util.List;

public class JobUpgradeNode {
  private final String name;
  private final int cost;
  private final List<JobUpgradeNode> neighbors;
  public JobUpgradeNode(String name, int cost, List<JobUpgradeNode> neighbors) {
    this.name = name;
    this.cost = cost;
    this.neighbors = neighbors;
  }

  public List<JobUpgradeNode> getNeighbors() {
    return neighbors;
  }
}
