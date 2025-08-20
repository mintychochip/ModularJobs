package net.aincraft.job;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.aincraft.Job;
import net.aincraft.Job.LevelingCurve;
import net.aincraft.Job.PayableCurve;
import net.aincraft.container.PayableType;
import net.aincraft.job.JobRecordRepository.JobRecord;
import net.aincraft.registry.Registry;
import net.aincraft.util.KeyFactory;
import net.aincraft.util.Mapper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

final class JobRecordMapperImpl implements Mapper<Job, JobRecord> {

  private final KeyFactory keyFactory;
  private final Registry<PayableType> payableTypeRegistry;
  private final Map<String, PayableCurve> payableCurveMap = new HashMap<>();
  private final Map<String, LevelingCurve> levelingCurveMap = new HashMap<>();

  @Inject
  JobRecordMapperImpl(KeyFactory keyFactory,
      Registry<PayableType> payableTypeRegistry) {
    this.keyFactory = keyFactory;
    this.payableTypeRegistry = payableTypeRegistry;
  }

  @Override
  public @NotNull Job toDomainObject(@NotNull JobRecord record) throws IllegalArgumentException {
    MiniMessage miniMessage = MiniMessage.miniMessage();
    Key jobKey = keyFactory.create(record.jobKey());
    Component displayName = miniMessage.deserialize(record.displayName());
    String rawDescription = record.description();
    Component description = rawDescription != null ? miniMessage.deserialize(rawDescription) : null;
    LevelingCurve levelingCurve = levelingCurveMap.computeIfAbsent(record.levellingCurve(),
        Exp4jLevelingCurveImpl::new);
    Map<PayableType, PayableCurve> payableCurves = payableCurves(record.payableCurves());
    return new JobImpl(jobKey, displayName, description, levelingCurve, payableCurves);
  }

  private Map<PayableType, PayableCurve> payableCurves(Map<String, String> curves)
      throws IllegalArgumentException {
    Map<PayableType, PayableCurve> payableCurves = new HashMap<>();
    for (Entry<String, String> payableCurve : curves.entrySet()) {
      String payableTypeKey = payableCurve.getKey();
      String curveFunction = payableCurve.getValue();
      if (payableTypeKey == null || curveFunction == null) {
        continue;
      }
      if (!payableTypeRegistry.isRegistered(keyFactory.create(payableTypeKey))) {
        continue;
      }
      PayableType type = payableTypeRegistry.getOrThrow(keyFactory.create(payableTypeKey));
      PayableCurve curve = payableCurveMap.computeIfAbsent(curveFunction,
          Exp4jPayableCurveImpl::new);
      payableCurves.put(type, curve);
    }
    return payableCurves;
  }
}
