package net.aincraft.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.aincraft.repository.ConnectionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Relational database implementation of PlayerUpgradeRepository.
 */
public final class PlayerUpgradeRepositoryImpl implements PlayerUpgradeRepository {

  private static final String SELECT_QUERY =
      "SELECT total_skill_points, unlocked_nodes, node_levels FROM player_upgrades WHERE player_id = ? AND job_key = ?";

  private static final String UPSERT_QUERY =
      "INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes, node_levels) " +
          "VALUES (?, ?, ?, ?, ?) " +
          "ON CONFLICT(player_id, job_key) DO UPDATE SET " +
          "total_skill_points = excluded.total_skill_points, " +
          "unlocked_nodes = excluded.unlocked_nodes, " +
          "node_levels = excluded.node_levels";

  private static final String DELETE_QUERY =
      "DELETE FROM player_upgrades WHERE player_id = ? AND job_key = ?";

  private static final String ADD_NODE_LEVELS_COLUMN =
      "ALTER TABLE player_upgrades ADD COLUMN node_levels TEXT NOT NULL DEFAULT ''";

  private final ConnectionSource connectionSource;
  private boolean migrationChecked = false;

  public PlayerUpgradeRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  /**
   * Ensure the node_levels column exists (migration for existing databases).
   */
  private synchronized void ensureMigration() {
    if (migrationChecked) {
      return;
    }
    migrationChecked = true;

    try (Connection connection = connectionSource.getConnection()) {
      // Check if node_levels column exists
      try (ResultSet rs = connection.getMetaData().getColumns(null, null, "player_upgrades", "node_levels")) {
        if (!rs.next()) {
          // Column doesn't exist, add it
          try (PreparedStatement ps = connection.prepareStatement(ADD_NODE_LEVELS_COLUMN)) {
            ps.executeUpdate();
          }
        }
      }
    } catch (SQLException e) {
      // Log but don't fail - column might already exist or table might not exist yet
      System.err.println("[ModularJobs] Migration check for node_levels column: " + e.getMessage());
    }
  }

  @Override
  public @Nullable PlayerUpgradeDataImpl loadPlayerData(@NotNull String playerId, @NotNull String jobKey) {
    ensureMigration();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_QUERY)) {

      ps.setString(1, playerId);
      ps.setString(2, jobKey);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int totalSkillPoints = rs.getInt("total_skill_points");
          String unlockedNodesStr = rs.getString("unlocked_nodes");
          String nodeLevelsStr = rs.getString("node_levels");
          Set<String> unlockedNodes = parseNodeSet(unlockedNodesStr);
          Map<String, Integer> nodeLevels = parseNodeLevels(nodeLevelsStr);
          return new PlayerUpgradeDataImpl(playerId, jobKey, totalSkillPoints, unlockedNodes, nodeLevels);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load player upgrade data for " + playerId + "/" + jobKey, e);
    }
    return null;
  }

  @Override
  public void savePlayerData(@NotNull PlayerUpgradeDataImpl data) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(UPSERT_QUERY)) {

      ps.setString(1, data.playerId());
      ps.setString(2, data.jobKey());
      ps.setInt(3, data.totalSkillPoints());
      ps.setString(4, serializeNodeSet(data.unlockedNodes()));
      ps.setString(5, serializeNodeLevels(data.nodeLevels()));

      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save player upgrade data for " + data.playerId() + "/" + data.jobKey(), e);
    }
  }

  @Override
  public boolean deletePlayerData(@NotNull String playerId, @NotNull String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(DELETE_QUERY)) {

      ps.setString(1, playerId);
      ps.setString(2, jobKey);

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete player upgrade data for " + playerId + "/" + jobKey, e);
    }
  }

  private Set<String> parseNodeSet(String str) {
    if (str == null || str.isBlank()) {
      return new HashSet<>();
    }
    return Arrays.stream(str.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toCollection(HashSet::new));
  }

  private String serializeNodeSet(Set<String> nodes) {
    if (nodes == null || nodes.isEmpty()) {
      return "";
    }
    return String.join(",", nodes);
  }

  /**
   * Parse node levels from string format "nodeKey1:2,nodeKey2:3".
   */
  private Map<String, Integer> parseNodeLevels(String str) {
    Map<String, Integer> levels = new HashMap<>();
    if (str == null || str.isBlank()) {
      return levels;
    }
    for (String entry : str.split(",")) {
      String[] parts = entry.split(":");
      if (parts.length == 2) {
        try {
          String nodeKey = parts[0].trim();
          int level = Integer.parseInt(parts[1].trim());
          if (!nodeKey.isEmpty() && level > 0) {
            levels.put(nodeKey, level);
          }
        } catch (NumberFormatException ignored) {
          // Skip malformed entries
        }
      }
    }
    return levels;
  }

  /**
   * Serialize node levels to string format "nodeKey1:2,nodeKey2:3".
   */
  private String serializeNodeLevels(Map<String, Integer> levels) {
    if (levels == null || levels.isEmpty()) {
      return "";
    }
    return levels.entrySet().stream()
        .filter(e -> e.getValue() > 1) // Only store levels > 1 (level 1 is implicit for unlocked nodes)
        .map(e -> e.getKey() + ":" + e.getValue())
        .collect(Collectors.joining(","));
  }
}
