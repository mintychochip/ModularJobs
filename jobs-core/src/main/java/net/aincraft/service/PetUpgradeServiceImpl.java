package net.aincraft.service;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.aincraft.repository.ConnectionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PetUpgradeServiceImpl implements PetUpgradeService {

  private static final String INSERT_QUERY = """
      INSERT INTO job_pet_selections (player_id, job_key, pet_config_name, selected_at)
      VALUES (?, ?, ?, ?)
      ON CONFLICT (player_id, job_key)
      DO UPDATE SET pet_config_name = excluded.pet_config_name, selected_at = excluded.selected_at
      """;

  private static final String SELECT_QUERY = """
      SELECT pet_config_name FROM job_pet_selections
      WHERE player_id = ? AND job_key = ?
      LIMIT 1
      """;

  private final ConnectionSource connectionSource;
  private final ConcurrentMap<String, List<String>> jobPetsMap;

  @Inject
  public PetUpgradeServiceImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
    this.jobPetsMap = new ConcurrentHashMap<>();
  }

  @Override
  public boolean hasUnlockedUpgrade(@NotNull UUID playerId, @NotNull String jobKey) {
    List<String> availablePets = jobPetsMap.get(jobKey);
    if (availablePets == null || availablePets.isEmpty()) {
      return false;
    }

    String selectedPet = getSelectedPet(playerId, jobKey);
    return selectedPet != null;
  }

  @Override
  public void setSelectedPet(@NotNull UUID playerId, @NotNull String jobKey,
      @NotNull String petConfigName) {
    List<String> availablePets = jobPetsMap.get(jobKey);
    if (availablePets == null || !availablePets.contains(petConfigName)) {
      throw new IllegalArgumentException(
          "Pet config '" + petConfigName + "' is not registered for job '" + jobKey + "'");
    }

    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(INSERT_QUERY)) {
      ps.setString(1, playerId.toString());
      ps.setString(2, jobKey);
      ps.setString(3, petConfigName);
      ps.setTimestamp(4, Timestamp.from(Instant.now()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to set selected pet for player " + playerId, e);
    }
  }

  @Override
  public @Nullable String getSelectedPet(@NotNull UUID playerId, @NotNull String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_QUERY)) {
      ps.setString(1, playerId.toString());
      ps.setString(2, jobKey);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("pet_config_name");
        }
      }
      return null;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get selected pet for player " + playerId, e);
    }
  }

  @Override
  public @NotNull List<String> getAvailablePets(@NotNull String jobKey) {
    List<String> pets = jobPetsMap.get(jobKey);
    return pets != null ? new ArrayList<>(pets) : List.of();
  }

  @Override
  public void registerJobPets(@NotNull String jobKey, @NotNull List<String> petConfigNames) {
    if (petConfigNames == null || petConfigNames.isEmpty()) {
      throw new IllegalArgumentException("Pet config names cannot be null or empty");
    }
    jobPetsMap.put(jobKey, new ArrayList<>(petConfigNames));
  }
}
