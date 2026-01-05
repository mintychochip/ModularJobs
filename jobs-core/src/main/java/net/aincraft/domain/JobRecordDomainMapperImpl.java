package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.math.ExpressionCurveFactory;
import net.aincraft.registry.Registry;
import net.aincraft.util.DomainMapper;
import net.aincraft.util.KeyUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class JobRecordDomainMapperImpl implements DomainMapper<Job, JobRecord> {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  private final DomainMapper<Map<Key, PayableCurve>, Map<String, String>> payableCurveMapper;
  private final Plugin plugin;
  private final Registry<PayableType> payableTypeRegistry;
  private final ExpressionCurveFactory expressionCurveFactory;

  @Inject
  JobRecordDomainMapperImpl(
      DomainMapper<Map<Key, PayableCurve>, Map<String, String>> payableCurveMapper,
      Plugin plugin,
      Registry<PayableType> payableTypeRegistry,
      ExpressionCurveFactory expressionCurveFactory) {
    this.payableCurveMapper = payableCurveMapper;
    this.plugin = plugin;
    this.payableTypeRegistry = payableTypeRegistry;
    this.expressionCurveFactory = expressionCurveFactory;
  }

  @Override
  public @NotNull Job toDomain(@NotNull JobRecord record) throws IllegalArgumentException {
    Key jobKey = KeyUtils.parseKey(plugin, record.jobKey());
    Component displayName = MINI_MESSAGE.deserialize(record.displayName());
    Component description = MINI_MESSAGE.deserialize(record.description());
    LevelingCurve levelingCurve = expressionCurveFactory.levelingCurve(record.levellingCurve());
    return new JobImpl(jobKey, displayName, description, record.maxLevel(), levelingCurve,
        payableCurveMapper.toDomain(record.payableCurves()));
  }

  @Override
  public @NotNull JobRecord toRecord(@NotNull Job domain) {
    String jobKey = domain.key().toString();
    String displayName = MINI_MESSAGE.serialize(domain.displayName());
    String description = MINI_MESSAGE.serialize(domain.description());
    LevelingCurve levelingCurve = domain.levelingCurve();
    return new JobRecord(jobKey, displayName, description, domain.maxLevel(),
        levelingCurve.toString(), payableCurveMapper.toRecord(domain.payableCurves()));
  }

  static final class PayableCurveMapperImpl implements
      DomainMapper<Map<Key, PayableCurve>, Map<String, String>> {

    private final Registry<PayableType> payableTypeRegistry;
    private final ExpressionCurveFactory expressionCurveFactory;
    private final Plugin plugin;

    @Inject
    PayableCurveMapperImpl(Registry<PayableType> payableTypeRegistry,
        ExpressionCurveFactory expressionCurveFactory, Plugin plugin) {
      this.payableTypeRegistry = payableTypeRegistry;
      this.expressionCurveFactory = expressionCurveFactory;
      this.plugin = plugin;
    }


    @Override
    public @NotNull Map<Key, PayableCurve> toDomain(@NotNull Map<String, String> record)
        throws IllegalArgumentException {
      Map<Key, PayableCurve> curves = new HashMap<>();
      for (Map.Entry<String, String> entry : record.entrySet()) {
        Key payableTypeKey = KeyUtils.parseKey(plugin, entry.getKey());
        if (!payableTypeRegistry.isRegistered(payableTypeKey)) {
          continue;
        }
        PayableCurve curve = expressionCurveFactory.payableCurve(entry.getValue());
        curves.put(payableTypeKey, curve);
      }
      return curves;
    }

    @Override
    public @NotNull Map<String, String> toRecord(@NotNull Map<Key, PayableCurve> domain) {
      Map<String, String> curves = new HashMap<>();
      for (Map.Entry<Key, PayableCurve> entry : domain.entrySet()) {
        curves.put(entry.getKey().toString(), entry.getValue().toString());
      }
      return curves;
    }
  }
}
