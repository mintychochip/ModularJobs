package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.aincraft.Job;
import net.aincraft.Job.LevelingCurve;
import net.aincraft.Job.PayableCurve;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.math.JobCurveFactory;
import net.aincraft.registry.Registry;
import net.aincraft.util.KeyFactory;
import net.aincraft.util.DomainMapper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

final class JobRecordDomainMapperImpl implements DomainMapper<Job, JobRecord> {

  private final KeyFactory keyFactory;
  private final Registry<PayableType> payableTypeRegistry;
  private final JobCurveFactory jobCurveFactory;

  @Inject
  JobRecordDomainMapperImpl(KeyFactory keyFactory,
      Registry<PayableType> payableTypeRegistry,
      JobCurveFactory jobCurveFactory) {
    this.keyFactory = keyFactory;
    this.payableTypeRegistry = payableTypeRegistry;
    this.jobCurveFactory = jobCurveFactory;
  }

  @Override
  public @NotNull Job toDomainObject(@NotNull JobRecord record) throws IllegalArgumentException {
    MiniMessage miniMessage = MiniMessage.miniMessage();
    Key jobKey = keyFactory.create(record.jobKey());
    Component displayName = miniMessage.deserialize(record.displayName());
    String rawDescription = record.description();
    Component description = rawDescription != null ? miniMessage.deserialize(rawDescription) : null;
    LevelingCurve levelingCurve = jobCurveFactory.levelingCurve(record.levellingCurve());
    Map<PayableType, PayableCurve> payableCurves = payableCurves(record.payableCurves());
    return new JobImpl(jobKey, displayName, description, record.maxLevel(), levelingCurve,
        payableCurves);
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
