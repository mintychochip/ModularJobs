package net.aincraft.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.Payable;
import net.aincraft.api.context.Context;
import net.aincraft.api.service.JobTaskProvider;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Format: job_key,action_type_key,context_key,payable_type_key,amount
 */
public class CSVJobTaskProviderImpl implements JobTaskProvider {

  private static final String JOB_TASK_CSV_PATH = "job_tasks.csv";

  private final Path csvPath;
  private final Map<CompositeJobKey,List<Payable>> payables = new HashMap<>();
  public CSVJobTaskProviderImpl(Path csvPath) {
    this.csvPath = csvPath;
  }

  public static JobTaskProvider create(Plugin plugin) {
    File dataFolder = plugin.getDataFolder();
    Path csvPath = dataFolder.toPath().resolve(JOB_TASK_CSV_PATH);
    if (!Files.exists(csvPath)) {
      dataFolder.mkdirs();
      plugin.saveResource(JOB_TASK_CSV_PATH,false);
    }
    return new CSVJobTaskProviderImpl(csvPath);
  }

  @Override
  public JobTask getTask(Job job, ActionType type, Context context) throws IOException {
    if (Files.exists(csvPath)) {
      List<String> lines = Files.readAllLines(csvPath);
      for (String line : lines.stream().skip(1).toList()) {
        String[] split = line.split(",");
        Key jobKey = NamespacedKey.fromString(split[0]);
        Key actionTypeKey = NamespacedKey.fromString(split[1]);
        Key contextKey = NamespacedKey.fromString(split[2]);
        Key payableTypeKey = NamespacedKey.fromString(split[3]);
      }
    }
    return null;
  }

  @Override
  public void addTask(Job job, ActionType type, Context object, List<Payable> payables) {

  }

  private record CompositeJobKey(Key jobKey, Key actionTypeKey, Key contextKey) {}
}
