package net.aincraft.boost;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import net.aincraft.boost.conditions.SnapshotCondition;
import net.aincraft.common.boost.BoostDataDocument;
import net.aincraft.common.boost.BoostDataDocument.BoostDocument;
import net.aincraft.common.boost.BoostDataDocument.RuleDocument;
import net.aincraft.common.boost.BoostDataDocument.SourceDocument;
import dev.mintychochip.databag.ConditionSerializer;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.factories.BoostFactory;
import net.kyori.adventure.key.Key;

/**
 * JSON codec for {@link SerializableBoostData}. Conditions are serializer bytes
 * on each rule.
 */
public final class BoostDataCodec {

  private final ConditionSerializer conditions;
  private final BoostFactory boosts;

  public BoostDataCodec(ConditionSerializer conditions, BoostFactory boosts) {
    this.conditions = conditions;
    this.boosts = boosts;
  }

  /**
   * Encodes {@code data} as UTF-8 JSON bytes.
   */
  public byte[] write(SerializableBoostData data) {
    String kind;
    String slots = null;
    String duration = null;
    if (data instanceof PassiveBoostData passive) {
      kind = "passive";
      slots = Base64.getEncoder().encodeToString(passive.slotSet().toByteArray());
    } else if (data instanceof ConsumableBoostData consumable) {
      kind = "consumable";
      duration = consumable.duration().toString();
    } else {
      throw new IllegalArgumentException("Unknown boost data: " + data.getClass().getName());
    }
    return BoostDataDocument.toJson(
        new BoostDataDocument(kind, slots, duration, toSource(data.boostSource())));
  }

  /**
   * Decodes UTF-8 JSON bytes into {@link SerializableBoostData}.
   */
  public SerializableBoostData read(byte[] bytes) {
    BoostDataDocument document = BoostDataDocument.fromJson(bytes);
    BoostSource source = fromSource(document.source());
    if ("passive".equalsIgnoreCase(document.kind())) {
      BitSet slotSet = new BitSet();
      if (document.slots() != null && !document.slots().isBlank()) {
        slotSet = BitSet.valueOf(Base64.getDecoder().decode(document.slots()));
      }
      return new PassiveBoostData(source, slotSet);
    }
    Duration duration = document.duration() == null || document.duration().isBlank()
        ? Duration.ZERO
        : Duration.parse(document.duration());
    return new ConsumableBoostData(source, duration);
  }

  public BoostSource readSource(byte[] bytes) {
    return fromSource(BoostDataDocument.fromJson(bytes).source());
  }

  private SourceDocument toSource(BoostSource source) {
    List<RuleDocument> rules = new ArrayList<>();
    if (source instanceof RuledBoostSource ruled) {
      for (Rule rule : ruled.rules()) {
        byte[] conditionBytes = conditions.write(SnapshotCondition.unwrap(rule.condition()));
        rules.add(RuleDocument.of(rule.priority(), conditionBytes, toBoost(rule.boost())));
      }
    }
    String key = source.key() != null ? source.key().asString() : "modularjobs:unknown";
    return new SourceDocument(key, source.description(), rules);
  }

  private BoostSource fromSource(SourceDocument document) {
    List<Rule> rules = new ArrayList<>();
    if (document.rules() != null) {
      for (RuleDocument rule : document.rules()) {
        net.aincraft.container.boost.Condition condition =
            SnapshotCondition.wrap(conditions.read(rule.conditionBytes()));
        rules.add(new Rule(condition, rule.priority(), fromBoost(rule.boost())));
      }
    }
    return new RuledBoostSourceImpl(
        rules, Key.key(document.key()), document.description());
  }

  private static BoostDocument toBoost(Boost boost) {
    return switch (boost) {
      case MultiplicativeBoostImpl mult ->
          new BoostDocument("multiplicative", mult.amount().doubleValue());
      case AdditiveBoostImpl add ->
          new BoostDocument("additive", add.amount().doubleValue());
      default -> throw new IllegalArgumentException(
          "Cannot serialize boost type: " + boost.getClass().getName());
    };
  }

  private Boost fromBoost(BoostDocument document) {
    BigDecimal amount = BigDecimal.valueOf(document.amount());
    return switch (document.type().toLowerCase()) {
      case "multiplicative" -> boosts.multiplicative(amount);
      case "additive" -> boosts.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + document.type());
    };
  }
}
