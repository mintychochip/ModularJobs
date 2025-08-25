package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.aincraft.Job;
import net.aincraft.Job.LevelingCurve;
import net.aincraft.Job.PayableCurve;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.ActionTypeRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.job.Exp4jLevelingCurveImpl;
import net.aincraft.job.Exp4jPayableCurveImpl;
import net.aincraft.job.JobImpl;
import net.aincraft.math.CurveFactory;
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
  private final Mapper<ActionType, ActionTypeRecord> actionTypeMapper;
  private final Mapper<JobTask, JobTaskRecord> jobTaskMapper;
  private final CurveFactory curveFactory;
  private final Map<String, PayableCurve> payableCurveMap = new HashMap<>();
  private final Map<String, LevelingCurve> levelingCurveMap = new HashMap<>();

  @Inject
  JobRecordMapperImpl(KeyFactory keyFactory,
      Registry<PayableType> payableTypeRegistry,
      Mapper<ActionType, ActionTypeRecord> actionTypeMapper,
      Mapper<JobTask, JobTaskRecord> jobTaskMapper, CurveFactory curveFactory) {
    this.keyFactory = keyFactory;
    this.payableTypeRegistry = payableTypeRegistry;
    this.actionTypeMapper = actionTypeMapper;
    this.jobTaskMapper = jobTaskMapper;
    this.curveFactory = curveFactory;
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

  private Map<ActionType,List<JobTask>> actionTypeTasks(Map<ActionTypeRecord, List<JobTask>>)

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
