package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class MemoryJobRepositoryImpl implements JobRepository {

  private final Map<String, JobRecord> records;

  @Inject
  MemoryJobRepositoryImpl(Map<String, JobRecord> records) {
    this.records = records;
  }

  @Override
  public @NotNull List<JobRecord> getJobs() {
    return records.values().stream().toList();
  }

  @Override
  public @Nullable JobRecord load(String jobKey) {
    return records.get(jobKey);
  }

  static final class YamlRecordLoader {

    //TODO: throw errors when you cannot load it
    public Map<String, JobRecord> load(YamlConfiguration configuration) {
      Map<String, JobRecord> jobs = new HashMap<>();
      for (String jobKey : configuration.getKeys(false)) {
        if (!configuration.contains(jobKey)) {
          continue;
        }
        ConfigurationSection jobConfiguration = configuration.getConfigurationSection(jobKey);
        assert jobConfiguration != null;
        String displayName = jobConfiguration.getString("display-name");
        if (displayName == null) {
          continue;
        }
        String description = jobConfiguration.getString("description", null);
        //TODO: get default max level
        int maxLevel = jobConfiguration.getInt("max-level", 1);
        String levellingCurve = jobConfiguration.getString("leveling-curve");
        if (levellingCurve == null) {
          continue;
        }
        ConfigurationSection curveConfiguration = jobConfiguration.getConfigurationSection(
            "payable-curves");
        Map<String, String> curves = new HashMap<>();
        for (String curveKey : curveConfiguration.getKeys(false)) {
          String curve = curveConfiguration.getString(curveKey);
          curves.put(curveKey, curve);
        }
        
        // Parse upgrade-level with default of 30
        int upgradeLevel = jobConfiguration.getInt("upgrade-level", 30);
        
        // Parse perk-unlocks map
        Map<Integer, List<String>> perkUnlocks = new HashMap<>();
        if (jobConfiguration.contains("perk-unlocks")) {
          ConfigurationSection perkUnlocksSection = jobConfiguration.getConfigurationSection("perk-unlocks");
          if (perkUnlocksSection != null) {
            for (String levelKey : perkUnlocksSection.getKeys(false)) {
              try {
                int level = Integer.parseInt(levelKey);
                List<String> perks = perkUnlocksSection.getStringList(levelKey);
                if (perks != null && !perks.isEmpty()) {
                  perkUnlocks.put(level, perks);
                }
              } catch (NumberFormatException e) {
                // Skip invalid level keys
              }
            }
          }
        }

        // Parse pet-perks map (pet name -> level -> perks) and pet revokes
        Map<String, Map<Integer, List<String>>> petPerks = new HashMap<>();
        Map<String, List<String>> petRevokedPerks = new HashMap<>();
        if (jobConfiguration.contains("pet-perks")) {
          ConfigurationSection petPerksSection = jobConfiguration.getConfigurationSection("pet-perks");
          if (petPerksSection != null) {
            for (String petName : petPerksSection.getKeys(false)) {
              ConfigurationSection petSection = petPerksSection.getConfigurationSection(petName);
              if (petSection != null) {
                // Check for revokes list
                if (petSection.contains("revokes")) {
                  List<String> revokes = petSection.getStringList("revokes");
                  if (revokes != null && !revokes.isEmpty()) {
                    petRevokedPerks.put(petName, revokes);
                  }
                }

                // Parse level -> perks (skip "revokes" key)
                Map<Integer, List<String>> petLevelPerks = new HashMap<>();
                for (String levelKey : petSection.getKeys(false)) {
                  if (levelKey.equals("revokes")) {
                    continue; // Skip revokes when parsing level -> perks
                  }
                  try {
                    int level = Integer.parseInt(levelKey);
                    List<String> perks = petSection.getStringList(levelKey);
                    if (perks != null && !perks.isEmpty()) {
                      petLevelPerks.put(level, perks);
                    }
                  } catch (NumberFormatException e) {
                    // Skip invalid level keys
                  }
                }
                if (!petLevelPerks.isEmpty()) {
                  petPerks.put(petName, petLevelPerks);
                }
              }
            }
          }
        }

        jobs.put("modularjobs:" + jobKey,
            new JobRecord("modularjobs:" + jobKey, displayName, description, maxLevel,
                levellingCurve, curves, upgradeLevel, perkUnlocks, petPerks, petRevokedPerks));
      }
      return jobs;
    }
  }
}
