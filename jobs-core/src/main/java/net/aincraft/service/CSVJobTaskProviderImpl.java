package net.aincraft.service;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.container.Currency;
import net.aincraft.container.KeyResolver;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableType;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Format: job_key,action_type_key,context_key,payable_type_key,amount
 */
public class CSVJobTaskProviderImpl implements JobTaskProvider {

  private static final String JOB_TASK_CSV_PATH = "job_tasks.csv";

  private final Path csvPath;
  private final Map<JobTaskKey, List<PayableRecord>> payables;

  private CSVJobTaskProviderImpl(Path csvPath, Map<JobTaskKey, List<PayableRecord>> payables) {
    this.csvPath = csvPath;
    this.payables = payables;
  }

  public static JobTaskProvider create(Plugin plugin) throws IOException {
    File dataFolder = plugin.getDataFolder();
    Path csvPath = dataFolder.toPath().resolve(JOB_TASK_CSV_PATH);
    if (!Files.exists(csvPath)) {
      dataFolder.mkdirs();
      plugin.saveResource(JOB_TASK_CSV_PATH, false);
    }
    Map<JobTaskKey, List<PayableRecord>> payables = new HashMap<>();
    List<String> lines = Files.readAllLines(csvPath);
    for (String line : lines.stream().skip(1).toList()) {
      String[] split = line.split(",");
      JobTaskKey key = JobTaskKey.create(split[0], split[1], split[2]);
      PayableRecord record = new PayableRecord(NamespacedKey.fromString(split[3]), split[4],
          split[5]);
      payables.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
    }
    return new CSVJobTaskProviderImpl(csvPath, payables);
  }

  @Override
  public boolean hasTask(Job job, ActionType type, Context context) {
    JobTaskKey key = JobTaskKey.create(job, type, context);
    return payables.containsKey(key);
  }

  @Override
  public JobTask getTask(Job job, ActionType type, Context context)
      throws IllegalArgumentException {
    JobTaskKey key = JobTaskKey.create(job, type, context);
    Preconditions.checkArgument(payables.containsKey(key));
    List<PayableRecord> records = payables.get(key);
    return () -> records.stream().map(PayableRecord::toPayable).toList();
  }

  @Override
  public void addTask(Job job, ActionType type, Context context, List<Payable> payables) {
    StringBuilder base = new StringBuilder()
        .append(job.key()).append(',')
        .append(type.key()).append(',')
        .append(KeyResolver.keyResolver().resolve(context)).append(',');
    for (Payable payable : payables) {
      PayableAmount amount = payable.amount();
      Optional<Currency> currency = amount.currency();
      BigDecimal bigDecimal = amount.amount();
      StringBuilder payableString = new StringBuilder(base)
          .append(payable.type().key()).append(',')
          .append(bigDecimal.toString());
      currency.ifPresent(c -> payableString.append(',').append(c.identifier()));
      try {
        Files.writeString(csvPath, payableString.toString(), StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    JobTaskKey key = JobTaskKey.create(job, type, context);
    List<PayableRecord> records = payables.stream().map(
        payable -> {
          PayableAmount amount = payable.amount();
          String currencyString = amount.currency().map(Currency::identifier).orElse(null);
          return new PayableRecord(payable.type().key(),
              amount.amount().toString(), currencyString);
        }).toList();
    this.payables.put(key, records);
  }

  private record JobTaskKey(Key jobKey, Key actionTypeKey, Key contextKey) {

    static JobTaskKey create(Job job, ActionType type, Context context) {
      Key contextKey = KeyResolver.keyResolver().resolve(context);
      return new JobTaskKey(job.key(), type.key(), contextKey);
    }

    static JobTaskKey create(String jobKeyString, String actionTypeKeyString,
        String contextKeyString) throws IllegalArgumentException {
      NamespacedKey jobKey = NamespacedKey.fromString(jobKeyString);
      NamespacedKey actionTypeKey = NamespacedKey.fromString(actionTypeKeyString);
      NamespacedKey contextKey = NamespacedKey.fromString(contextKeyString);
      if (jobKey == null || actionTypeKey == null || contextKey == null) {
        throw new IllegalArgumentException(
            "Invalid parameters: all keys must be valid namespaced keys");
      }
      return new JobTaskKey(jobKey, actionTypeKey, contextKey);
    }
  }

  private record PayableRecord(Key payableTypeKey, String amount, String currency) {

    Payable toPayable() throws IllegalStateException {
      RegistryView<PayableType> registry = RegistryContainer.registryContainer()
          .getRegistry(RegistryKeys.PAYABLE_TYPES);
      try {
        return Payable.create(registry.getOrThrow(payableTypeKey), PayableAmount.create(
            new BigDecimal(amount), new net.aincraft.container.Currency() {
              @Override
              public String identifier() {
                return currency;
              }

              @Override
              public String symbol() {
                return "*";
              }
            }));
      } catch (IllegalArgumentException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }
}
