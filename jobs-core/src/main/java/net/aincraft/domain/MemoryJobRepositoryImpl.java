package net.aincraft.domain;

import com.google.inject.Inject;
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
  public @Nullable JobRecord getJob(String jobKey) {
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
        jobs.put("modularjobs:" + jobKey,
            new JobRecord("modularjobs:" + jobKey, displayName, description, maxLevel,
                levellingCurve, curves));
      }
      return jobs;
    }
  }
}
