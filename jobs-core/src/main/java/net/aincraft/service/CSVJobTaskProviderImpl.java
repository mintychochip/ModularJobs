//package net.aincraft.service;
//
//import java.io.File;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import net.aincraft.Job;
//import net.aincraft.JobTask;
//import net.aincraft.container.ActionType;
//import net.aincraft.container.Context;
//import net.aincraft.container.Payable;
//import net.aincraft.container.PayableAmount;
//import net.aincraft.container.PayableType;
//import net.aincraft.registry.RegistryContainer;
//import net.aincraft.registry.RegistryKeys;
//import net.aincraft.registry.RegistryView;
//import net.aincraft.util.KeyResolver;
//import net.kyori.adventure.key.Key;
//import org.bukkit.NamespacedKey;
//import org.bukkit.plugin.Plugin;
//
///**
// * Format: job_key,action_type_key,context_key,payable_type_key,amount
// */
//final class CSVJobTaskProviderImpl implements JobTaskProvider {
//
//  private static final String JOB_TASK_CSV_PATH = "job_tasks.csv";
//
//  private final KeyResolver keyResolver;
//  private final Path csvPath;
//  private final Map<JobTaskKey, List<PayableRecord>> payables;
//
//  CSVJobTaskProviderImpl(KeyResolver keyResolver, Path csvPath,
//      Map<JobTaskKey, List<PayableRecord>> payables) {
//    this.keyResolver = keyResolver;
//    this.csvPath = csvPath;
//    this.payables = payables;
//  }
//
//  public static JobTaskProvider create(Plugin plugin, KeyResolver keyResolver) throws IOException {
//    File dataFolder = plugin.getDataFolder();
//    Path csvPath = dataFolder.toPath().resolve(JOB_TASK_CSV_PATH);
//    if (!Files.exists(csvPath)) {
//      dataFolder.mkdirs();
//      plugin.saveResource(JOB_TASK_CSV_PATH, false);
//    }
//    Map<JobTaskKey, List<PayableRecord>> payables = new HashMap<>();
//    List<String> lines = Files.readAllLines(csvPath);
//    for (String line : lines.stream().skip(1).toList()) {
//      String[] split = line.split(",");
//      JobTaskKey key = new JobTaskKey(NamespacedKey.fromString(split[0]),
//          NamespacedKey.fromString(split[1]), NamespacedKey.fromString(split[2]));
//      PayableRecord record = new PayableRecord(NamespacedKey.fromString(split[3]), split[4],
//          split[5]);
//      payables.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
//    }
//    return new CSVJobTaskProviderImpl(keyResolver, csvPath, payables);
//  }
//
//  @Override
//  public Optional<JobTask> getTask(Job job, ActionType type, Context context)
//      throws IllegalArgumentException {
//    Key contextKey = keyResolver.resolve(context);
//    JobTaskKey key = new JobTaskKey(job.key(), type.key(), contextKey);
//    if (!payables.containsKey(key)) {
//      return Optional.empty();
//    }
//    List<PayableRecord> records = payables.get(key);
//    return Optional.of(() -> records.stream().map(PayableRecord::toPayable).toList());
//  }
//
//  private record JobTaskKey(Key jobKey, Key actionTypeKey, Key contextKey) {
//
//  }
//
//  private record PayableRecord(Key payableTypeKey, String amount, String currencyIdentifier) {
//
//    Payable toPayable() throws IllegalStateException {
//      RegistryView<PayableType> registry = RegistryContainer.registryContainer()
//          .getRegistry(RegistryKeys.PAYABLE_TYPES);
//      try {
//        return new Payable(registry.getOrThrow(payableTypeKey), PayableAmount.create(
//            new BigDecimal(amount), new net.aincraft.container.Currency() {
//              @Override
//              public String identifier() {
//                return currencyIdentifier;
//              }
//
//              @Override
//              public String symbol() {
//                return "*";
//              }
//            }));
//      } catch (IllegalArgumentException ex) {
//        throw new IllegalStateException(ex);
//      }
//    }
//  }
//}
