package dev.mintychochip.domain;

import dev.mintychochip.Job;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.math.ExpressionCurves;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.util.KeyUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;

/**
 * Immutable job definition backed by a persistence record.
 *
 * @param key namespaced job identifier
 * @param displayName user-facing display name
 * @param description user-facing description
 * @param maxLevel highest attainable level
 * @param levelingCurve experience curve used to calculate levels
 * @param payableCurves reward curves keyed by payable type
 * @param upgradeLevel level at which upgrades become available
 * @param perkUnlocks perk permissions grouped by unlock level
 */
record JobImpl(
    Key key,
    Component displayName,
    Component description,
    int maxLevel,
    LevelingCurve levelingCurve,
    Map<Key, PayableCurve> payableCurves,
    int upgradeLevel,
    Map<Integer, List<String>> perkUnlocks)
    implements Job {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  /**
   * Returns the display name without Adventure formatting.
   *
   * @return plain-text display name
   */
  @Override
  public String getPlainName() {
    return PlainTextComponentSerializer.plainText().serialize(displayName);
  }

  /**
   * Converts this job to the serialized record used by persistence.
   *
   * @return record containing MiniMessage text and curve expressions
   */
  JobRecord toRecord() {
    return new JobRecord(
        key.toString(),
        MINI_MESSAGE.serialize(displayName),
        MINI_MESSAGE.serialize(description),
        maxLevel(),
        levelingCurve.toString(),
        serializePayableCurves(),
        upgradeLevel(),
        perkUnlocks());
  }

  /**
   * Reconstructs a job from persisted text and curve expressions.
   *
   * <p>Payable curves whose types are no longer registered are ignored.
   *
   * @param record persisted job data
   * @param plugin plugin supplying the default namespace for unqualified keys
   * @param payableTypeRegistry registry used to filter known payable types
   * @return reconstructed job
   * @throws IllegalArgumentException if a key or curve expression is invalid
   */
  static JobImpl fromRecord(
      JobRecord record, Plugin plugin, Registry<PayableType> payableTypeRegistry) {
    Map<Key, PayableCurve> curves = new HashMap<>();
    for (Map.Entry<String, String> entry : record.payableCurves().entrySet()) {
      Key payableTypeKey = KeyUtils.parseKey(plugin, entry.getKey());
      if (payableTypeRegistry.isRegistered(payableTypeKey)) {
        curves.put(payableTypeKey, ExpressionCurves.payableCurve(entry.getValue()));
      }
    }

    return new JobImpl(
        KeyUtils.parseKey(plugin, record.jobKey()),
        MINI_MESSAGE.deserialize(record.displayName()),
        MINI_MESSAGE.deserialize(record.description()),
        record.maxLevel(),
        ExpressionCurves.levelingCurve(record.levellingCurve()),
        curves,
        record.upgradeLevel(),
        record.perkUnlocks());
  }

  private Map<String, String> serializePayableCurves() {
    return payableCurves().entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
  }
}
