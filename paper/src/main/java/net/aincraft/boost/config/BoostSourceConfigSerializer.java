package net.aincraft.boost.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.conditions.SnapshotCondition;
import dev.mintychochip.databag.AllOfCondition;
import dev.mintychochip.databag.AlwaysCondition;
import dev.mintychochip.databag.AnyOfCondition;
import dev.mintychochip.databag.BiomeCondition;
import dev.mintychochip.databag.FluidCondition;
import dev.mintychochip.databag.InvertedCondition;
import dev.mintychochip.databag.JobCondition;
import dev.mintychochip.databag.PlayerResourceCondition;
import dev.mintychochip.databag.PotionAmplifierCondition;
import dev.mintychochip.databag.PotionPresentCondition;
import dev.mintychochip.databag.SneakingCondition;
import dev.mintychochip.databag.SprintingCondition;
import dev.mintychochip.databag.WeatherCondition;
import dev.mintychochip.databag.WorldCondition;
import net.aincraft.boost.config.BoostSourceConfig.BoostConfig;
import net.aincraft.boost.config.BoostSourceConfig.ConditionConfig;
import net.aincraft.boost.config.BoostSourceConfig.RuleConfig;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serializes runtime {@link BoostSource}/{@link Condition} graphs back to
 * {@link BoostSourceConfig} JSON models for editor export / round-trip.
 */
public final class BoostSourceConfigSerializer {

  private BoostSourceConfigSerializer() {}

  /** Serializes a whole boost source into its JSON configuration model. */
  public static BoostSourceConfig serialize(@NotNull BoostSource source) {
    String key = source.key() != null ? source.key().asString() : "modularjobs:unknown";
    String description = source.description();
    List<RuleConfig> rules = new ArrayList<>();

    if (source instanceof RuledBoostSource ruled) {
      for (Rule rule : ruled.rules()) {
        rules.add(serializeRule(rule));
      }
    }

    return new BoostSourceConfig(key, description, null, rules);
  }

  /**
   * Serialize only the rules list (for upgrade effect export).
   */
  public static List<RuleConfig> serializeRules(@NotNull BoostSource source) {
    if (source instanceof RuledBoostSource ruled) {
      List<RuleConfig> rules = new ArrayList<>();
      for (Rule rule : ruled.rules()) {
        rules.add(serializeRule(rule));
      }
      return rules;
    }
    return List.of();
  }

  /** Serializes a single boost rule into its JSON configuration model. */
  public static RuleConfig serializeRule(@NotNull Rule rule) {
    return new RuleConfig(
        rule.priority(),
        serializeCondition(rule.condition()),
        serializeBoost(rule.boost())
    );
  }

  /**
   * Serializes a boost into its type/amount model.
   *
   * @throws IllegalArgumentException for unsupported boost implementations
   */
  public static BoostConfig serializeBoost(@NotNull Boost boost) {
    return switch (boost) {
      case MultiplicativeBoostImpl mult ->
          new BoostConfig("multiplicative", mult.amount().doubleValue());
      case AdditiveBoostImpl add ->
          new BoostConfig("additive", add.amount().doubleValue());
      default -> throw new IllegalArgumentException(
          "Cannot serialize boost type: " + boost.getClass().getName());
    };
  }

  /**
   * Serializes a runtime condition into its JSON configuration model.
   *
   * @throws IllegalArgumentException for unsupported condition implementations
   */
  public static ConditionConfig serializeCondition(@NotNull Condition condition) {
    if (!(condition instanceof SnapshotCondition snapshot)) {
      throw new IllegalArgumentException(
          "Cannot serialize condition type: " + condition.getClass().getName());
    }
    return serializeDataBag(snapshot.delegate());
  }

  private static ConditionConfig serializeDataBag(
      dev.mintychochip.databag.Condition condition) {
    return switch (condition) {
      case AlwaysCondition ignored -> always();
      case AllOfCondition all -> {
        if (all.terms().isEmpty()) {
          yield always();
        }
        List<ConditionConfig> children = all.terms().stream()
            .map(BoostSourceConfigSerializer::serializeDataBag)
            .toList();
        yield new ConditionConfig(
            "and", null, null, null, children, null, null, null, null, null, null, null);
      }
      case AnyOfCondition any -> {
        List<ConditionConfig> children = any.terms().stream()
            .map(BoostSourceConfigSerializer::serializeDataBag)
            .toList();
        yield new ConditionConfig(
            "or", null, null, null, children, null, null, null, null, null, null, null);
      }
      case InvertedCondition inverted -> new ConditionConfig(
          "not", null, null, null, null, serializeDataBag(inverted.term()),
          null, null, null, null, null, null);
      case SneakingCondition sneak -> simple("sneaking", sneak.expected());
      case SprintingCondition sprint -> simple("sprinting", sprint.expected());
      case BiomeCondition biome -> simple("biome", biome.biomeKey().asString());
      case WorldCondition world -> simple("world", preferredWorldName(world.worldName()));
      case WeatherCondition weather ->
          simple("weather", weather.state().name().toLowerCase(Locale.ROOT));
      case FluidCondition fluid -> new ConditionConfig(
          "liquid", null, fluid.fluidKey().asString(), null, null, null,
          null, null, null, null, null, true);
      case PlayerResourceCondition resource -> new ConditionConfig(
          "player_resource",
          operatorName(mapOperator(resource.operator())),
          resource.expected(),
          null, null, null,
          resource.type().name().toLowerCase(Locale.ROOT),
          null, null, null, null, null);
      case PotionPresentCondition potion -> new ConditionConfig(
          "potion_effect", null, null, null, null, null, null,
          stripMinecraft(potion.effectKey().asString()),
          null, null, null, null);
      case PotionAmplifierCondition potion -> new ConditionConfig(
          "potion_effect",
          operatorName(mapOperator(potion.operator())),
          null, null, null, null, null,
          stripMinecraft(potion.effectKey().asString()),
          potion.expected(),
          null, null, null);
      case JobCondition job -> {
        List<String> keys = new ArrayList<>(job.jobKeys());
        if (keys.size() == 1) {
          yield simple("job", stripJobNamespace(keys.getFirst()));
        }
        List<Object> values = keys.stream()
            .map(BoostSourceConfigSerializer::stripJobNamespace)
            .map(s -> (Object) s)
            .toList();
        yield new ConditionConfig(
            "job", null, null, values, null, null, null, null, null, null, null, null);
      }
      default -> throw new IllegalArgumentException(
          "Cannot serialize condition type: " + condition.getClass().getName());
    };
  }

  private static String preferredWorldName(String worldName) {
    if (worldName.startsWith("minecraft:")) {
      return worldName.substring("minecraft:".length());
    }
    return worldName;
  }

  private static String stripMinecraft(String key) {
    return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
  }

  private static RelationalOperator mapOperator(
      dev.mintychochip.databag.RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> RelationalOperator.LESS_THAN;
      case LESS_THAN_OR_EQUAL -> RelationalOperator.LESS_THAN_OR_EQUAL;
      case GREATER_THAN -> RelationalOperator.GREATER_THAN;
      case GREATER_THAN_OR_EQUAL -> RelationalOperator.GREATER_THAN_OR_EQUAL;
      case EQUAL -> RelationalOperator.EQUAL;
      case NOT_EQUAL -> RelationalOperator.NOT_EQUAL;
    };
  }

  private static ConditionConfig always() {
    return new ConditionConfig(
        "always", null, null, null, null, null, null, null, null, null, null, null);
  }

  private static ConditionConfig simple(String type, @Nullable Object value) {
    return new ConditionConfig(
        type, null, value, null, null, null, null, null, null, null, null, null
    );
  }

  private static String operatorName(RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> "less_than";
      case LESS_THAN_OR_EQUAL -> "less_than_or_equal";
      case GREATER_THAN -> "greater_than";
      case GREATER_THAN_OR_EQUAL -> "greater_than_or_equal";
      case EQUAL -> "equal";
      case NOT_EQUAL -> "not_equal";
    };
  }

  private static String stripJobNamespace(String jobKey) {
    if (jobKey.startsWith("modularjobs:")) {
      return jobKey.substring("modularjobs:".length());
    }
    return jobKey;
  }

}
