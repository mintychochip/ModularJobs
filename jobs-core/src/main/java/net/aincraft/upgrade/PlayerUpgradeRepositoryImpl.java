package net.aincraft.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
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
      "SELECT total_skill_points, unlocked_nodes FROM player_upgrades WHERE player_id = ? AND job_key = ?";

  private static final String UPSERT_QUERY =
      "INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes) " +
          "VALUES (?, ?, ?, ?) " +
          "ON CONFLICT(player_id, job_key) DO UPDATE SET " +
          "total_skill_points = excluded.total_skill_points, " +
          "unlocked_nodes = excluded.unlocked_nodes";

  private static final String DELETE_QUERY =
      "DELETE FROM player_upgrades WHERE player_id = ? AND job_key = ?";

  private final ConnectionSource connectionSource;

  public PlayerUpgradeRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
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

  @Override
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
}
