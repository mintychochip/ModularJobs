package net.aincraft.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.domain.model.JobRecord;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MemoryJobRepositoryImpl {

  private final Map<String, JobRecord> records;

  MemoryJobRepositoryImpl(Map<String, JobRecord> records) {
    this.records = records;
  }

  public @NotNull List<JobRecord> getJobs() {
    return records.values().stream().toList();
  }

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

        jobs.put("modularjobs:" + jobKey,
            new JobRecord("modularjobs:" + jobKey, displayName, description, maxLevel,
                levellingCurve, curves, upgradeLevel, perkUnlocks));
      }
      return jobs;
    }
  }
}
