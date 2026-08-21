package dev.mintychochip.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.SqlStatements;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Relational database implementation of PlayerUpgradeRepository.
 */
public final class PlayerUpgradeRepository {

  private static final String SELECT_QUERY =
      SqlStatements.load("player_upgrades/select.sql");

  private static final String UPSERT_QUERY =
      SqlStatements.load("player_upgrades/upsert.sql");

  private static final String DELETE_QUERY =
      SqlStatements.load("player_upgrades/delete.sql");

  private static final String SELECT_STATE_QUERY =
      SqlStatements.load("player_upgrades/select-state.sql");

  private static final String UPSERT_STATE_QUERY =
      SqlStatements.load("player_upgrades/upsert-state.sql");

  private final ConnectionSource connectionSource;

  public PlayerUpgradeRepository(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
    // Schema is connect-only: node_levels must exist via sql/mysql.sql (ops apply).
  }

  public @Nullable PlayerUpgradeDataImpl loadPlayerData(@NotNull String playerId, @NotNull String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_QUERY)) {

      ps.setString(1, playerId);
      ps.setString(2, jobKey);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int totalSkillPoints = rs.getInt("total_skill_points");
          String unlockedNodesStr = rs.getString("unlocked_nodes");
          Set<String> unlockedNodes = parseNodeSet(unlockedNodesStr);
          return new PlayerUpgradeDataImpl(playerId, jobKey, totalSkillPoints, unlockedNodes);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load player upgrade data for " + playerId + "/" + jobKey, e);
    }
    return null;
  }

  public void savePlayerData(@NotNull PlayerUpgradeDataImpl data) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(UPSERT_QUERY)) {

      ps.setString(1, data.playerId());
      ps.setString(2, data.jobKey());
      ps.setInt(3, data.totalSkillPoints());
      ps.setString(4, serializeNodeSet(data.unlockedNodes()));

      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save player upgrade data for " + data.playerId() + "/" + data.jobKey(), e);
    }
  }

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

  /**
   * Load a player's skill tree state for a job (v2 format).
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return the skill tree state, or null if none exists
   */
  public @Nullable SkillTreeState loadState(@NotNull String playerId, @NotNull String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_STATE_QUERY)) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        int total = rs.getInt("total_skill_points");
        String nodeLevelsStr = rs.getString("node_levels");
        Map<String, Integer> nodeLevels;
        if (nodeLevelsStr == null || nodeLevelsStr.isBlank()) {
          // Legacy row: unlocked_nodes -> level 1 each
          nodeLevels = new HashMap<>();
          for (String key : parseNodeSet(rs.getString("unlocked_nodes"))) {
            nodeLevels.put(key, 1);
          }
        } else {
          nodeLevels = parseNodeLevels(nodeLevelsStr);
        }
        return new SkillTreeState(playerId, jobKey, total, nodeLevels, Map.of());
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load player skill tree state for " + playerId + "/" + jobKey, e);
    }
  }

  /**
   * Save a player's skill tree state for a job (v2 format).
   *
   * @param state the state to save
   */
  public void saveState(@NotNull SkillTreeState state) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(UPSERT_STATE_QUERY)) {
      ps.setString(1, state.playerId());
      ps.setString(2, state.jobKey());
      ps.setInt(3, state.totalSkillPoints());
      ps.setString(4, serializeNodeLevels(state.nodeLevels()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save player skill tree state for " + state.playerId() + "/" + state.jobKey(), e);
    }
  }

  /**
   * Hydrate a loaded state's derived state map from its purchased majors.
   * {@link #loadState} returns an empty state map; the service recomputes it
   * from {@code node_levels} + the tree so major state survives a restart.
   * {@code node_levels} is persisted as an unordered map, so replay follows
   * prerequisite order: a major whose purchase required an earlier major
   * replays after it, mirroring the order purchases could actually occur.
   */
  public static SkillTreeState hydrate(SkillTree tree, SkillTreeState persisted) {
    // Clamp persisted levels to the tree's real max so a bad or edited row
    // (negative/oversized level) cannot poison purchase or spent-point math.
    // Unknown keys are passed through untouched: the legacy-migration adapter
    // keeps a player's old node IDs visible until the tree is re-purchased.
    Map<String, Integer> normalized = new HashMap<>();
    for (Map.Entry<String, Integer> entry : persisted.nodeLevels().entrySet()) {
      SkillNode node = tree.node(entry.getKey()).orElse(null);
      if (node == null) {
        normalized.put(entry.getKey(), entry.getValue());
      } else {
        normalized.put(entry.getKey(), Math.max(1, Math.min(entry.getValue(), node.maxLevel())));
      }
    }

    Map<Key, String> hydrated = new HashMap<>();
    for (String nodeKey : ownedInPrerequisiteOrder(tree, normalized)) {
      SkillNode node = tree.node(nodeKey).orElse(null);
      if (node == null || !node.isMajor()) {
        continue;
      }
      for (NodeStateWrite write : node.stateWrites()) {
        if (write.op() == NodeStateWrite.Op.SET) {
          hydrated.put(write.key(), write.value());
        } else if (write.op() == NodeStateWrite.Op.REMOVE) {
          hydrated.remove(write.key());
        }
      }
      for (NodeEffect effect : node.effects()) {
        if (effect instanceof NodeEffect.StateSetEffect stateSet) {
          if (stateSet.remove()) {
            hydrated.remove(stateSet.key());
          } else {
            hydrated.put(stateSet.key(), stateSet.value());
          }
        }
      }
    }
    return new SkillTreeState(
        persisted.playerId(), persisted.jobKey(), persisted.totalSkillPoints(),
        normalized, hydrated,
        persisted.currentJobLevel(), persisted.permissionCheck());
  }

  /**
   * Owned node keys (level ≥ 1) ordered so every node appears after its
   * prerequisites. Nodes not reachable through a prerequisite chain keep a
   * stable relative order derived from the input map; prerequisite cycles are
   * tolerated by breaking them at the first repeated node.
   */
  private static List<String> ownedInPrerequisiteOrder(
      SkillTree tree, Map<String, Integer> levels) {
    List<String> ordered = new ArrayList<>();
    Set<String> done = new HashSet<>();
    Set<String> visiting = new HashSet<>();
    for (String nodeKey : levels.keySet()) {
      if (levels.getOrDefault(nodeKey, 0) >= 1) {
        visitPrerequisites(tree, levels, nodeKey, done, visiting, ordered);
      }
    }
    return ordered;
  }

  private static void visitPrerequisites(
      SkillTree tree, Map<String, Integer> levels, String nodeKey,
      Set<String> done, Set<String> visiting, List<String> ordered) {
    if (done.contains(nodeKey) || visiting.contains(nodeKey)
        || levels.getOrDefault(nodeKey, 0) < 1) {
      return;
    }
    visiting.add(nodeKey);
    tree.node(nodeKey).ifPresent(node -> {
      for (String prereq : node.prerequisites()) {
        visitPrerequisites(tree, levels, prereq, done, visiting, ordered);
      }
    });
    visiting.remove(nodeKey);
    done.add(nodeKey);
    ordered.add(nodeKey);
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

  private Map<String, Integer> parseNodeLevels(String str) {
    // JSON object {"node": level}; empty -> empty map
    if (str == null || str.isBlank()) {
      return new HashMap<>();
    }
    Map<String, Integer> result = new HashMap<>();
    com.google.gson.JsonObject obj =
        new com.google.gson.Gson().fromJson(str, com.google.gson.JsonObject.class);
    if (obj != null) {
      obj.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsInt()));
    }
    return result;
  }

  private String serializeNodeLevels(Map<String, Integer> nodeLevels) {
    if (nodeLevels == null || nodeLevels.isEmpty()) {
      return "";
    }
    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
    nodeLevels.forEach(obj::addProperty);
    return new com.google.gson.Gson().toJson(obj);
  }
}
