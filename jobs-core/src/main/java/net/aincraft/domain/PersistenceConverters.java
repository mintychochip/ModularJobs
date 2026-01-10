package net.aincraft.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.PayableCurve;
import net.aincraft.container.Currency;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.math.ExpressionCurves;
import net.aincraft.registry.Registry;
import net.aincraft.util.KeyUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for converting between domain objects and persistence records.
 * This lives in jobs-core to avoid circular dependencies with jobs-api.
 */
public final class PersistenceConverters {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  private PersistenceConverters() {}

  // ========== Job conversions ==========

  public static JobRecord toRecord(Job job) {
    if (!(job instanceof JobImpl jobImpl)) {
      throw new IllegalArgumentException("Job must be a JobImpl instance");
    }
    return jobImpl.toRecord();
  }

  public static Job fromRecord(JobRecord record, Plugin plugin, Registry<PayableType> payableTypeRegistry) {
    return JobImpl.fromRecord(record, plugin, payableTypeRegistry);
  }

  // ========== JobProgression conversions ==========

  public static JobProgressionRecord toRecord(JobProgression progression) {
    if (!(progression instanceof JobProgressionImpl progressionImpl)) {
      throw new IllegalArgumentException("JobProgression must be a JobProgressionImpl instance");
    }
    return progressionImpl.toRecord();
  }

  public static JobProgression fromRecord(
      JobProgressionRecord record,
      Plugin plugin,
      Registry<PayableType> payableTypeRegistry
  ) {
    return JobProgressionImpl.fromRecord(record, plugin, payableTypeRegistry);
  }

  // ========== JobTask conversions ==========

  public static JobTaskRecord toRecord(JobTask task) {
    return new JobTaskRecord(
        task.jobKey().toString(),
        task.actionTypeKey().toString(),
        task.contextKey().toString(),
        task.payables().stream().map(PersistenceConverters::toRecord).collect(Collectors.toList())
    );
  }

  public static JobTask fromRecord(
      JobTaskRecord record,
      Function<String, PayableType> typeResolver
  ) {
    return new JobTask(
        Key.key(record.jobKey()),
        Key.key(record.actionTypeKey()),
        Key.key(record.contextKey()),
        record.payables().stream()
            .map(p -> fromRecord(p, typeResolver))
            .collect(Collectors.toList())
    );
  }

  // ========== Payable conversions ==========

  public static PayableRecord toRecord(Payable payable) {
    return new PayableRecord(
        payable.type().key().toString(),
        payable.amount().value(),
        payable.amount().currency().map(Currency::identifier).orElse(null)
    );
  }

  public static Payable fromRecord(PayableRecord record, Function<String, PayableType> typeResolver) {
    NamespacedKey key = NamespacedKey.fromString(record.payableTypeKey());
    if (key == null) {
      throw new IllegalArgumentException("Invalid payable type key: " + record.payableTypeKey());
    }
    PayableType type = typeResolver.apply(record.payableTypeKey());
    PayableAmount amount = record.currencyIdentifier() != null
        ? PayableAmount.create(record.amount(), Currency.of(record.currencyIdentifier(), ""))
        : PayableAmount.create(record.amount());
    return new Payable(type, amount);
  }
}
