package dev.mintychochip.boost.config;

import java.util.ArrayList;
import java.util.List;
import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.boost.config.BoostSourceConfig.ConditionConfig;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import dev.mintychochip.container.boost.PlayerResourceType;
import dev.mintychochip.container.boost.PotionConditionType;
import dev.mintychochip.container.boost.RelationalOperator;
import dev.mintychochip.container.boost.WeatherState;
import dev.mintychochip.container.boost.factories.ConditionFactory;

/**
 * Parses ConditionConfig from JSON into Condition instances.
 */
public final class ConditionConfigParser {

  private final ConditionFactory conditionFactory;

  /**
   * Creates a parser delegating condition construction to {@code conditionFactory}.
   */
  public ConditionConfigParser(ConditionFactory conditionFactory) {
    this.conditionFactory = conditionFactory;
  }

  /**
   * Parses a {@link ConditionConfig} descriptor into a runtime {@link Condition}.
   *
   * @throws IllegalArgumentException when the condition type is unknown or misconfigured
   */
  public Condition parse(ConditionConfig config) {
    return switch (config.type().toLowerCase()) {
      case "always" -> SnapshotCondition.wrap(dev.mintychochip.databag.Conditions.always());
      case "biome" -> parseBiome(config);
      case "world" -> parseWorld(config);
      case "sneaking" -> parseSneaking(config);
      case "sprinting" -> parseSprinting(config);
      case "player_resource" -> parsePlayerResource(config);
      case "potion_effect" -> parsePotionEffect(config);
      case "liquid" -> parseLiquid(config);
      case "weather" -> parseWeather(config);
      case "job" -> parseJob(config);
      case "and" -> parseComposite(config, LogicalOperator.AND);
      case "or" -> parseComposite(config, LogicalOperator.OR);
      case "not" -> parseNegation(config);
      default -> throw new IllegalArgumentException("Unknown condition type: " + config.type());
    };
  }

  private Condition parseBiome(ConditionConfig config) {
    if (!(config.value() instanceof String biomeStr) || biomeStr.isBlank()) {
      throw new IllegalArgumentException("biome condition requires a non-empty string 'value'");
    }
    // String key: resolved at evaluation; no live biome registry at parse time
    return conditionFactory.biome(biomeStr);
  }

  /**
   * Parse world by name or key string. Does not require the world to exist at parse time;
   * matching is deferred to snapshot evaluation.
   */
  private Condition parseWorld(ConditionConfig config) {
    if (!(config.value() instanceof String worldName) || worldName.isBlank()) {
      throw new IllegalArgumentException("world condition requires a non-empty string 'value'");
    }
    return conditionFactory.world(worldName);
  }

  private Condition parseSneaking(ConditionConfig config) {
    boolean state = (boolean) config.value();
    return conditionFactory.sneaking(state);
  }

  private Condition parseSprinting(ConditionConfig config) {
    boolean state = (boolean) config.value();
    return conditionFactory.sprinting(state);
  }

  private Condition parsePlayerResource(ConditionConfig config) {
    String resourceTypeStr = config.resourceType();
    if (resourceTypeStr == null) {
      throw new IllegalArgumentException("player_resource condition requires 'resourceType'");
    }
    PlayerResourceType resourceType = parsePlayerResourceType(resourceTypeStr);

    String operatorStr = config.operator();
    if (operatorStr == null) {
      throw new IllegalArgumentException("player_resource condition requires 'operator'");
    }
    RelationalOperator operator = parseRelationalOperator(operatorStr);

    double value;
    if (config.value() instanceof Number num) {
      value = num.doubleValue();
    } else {
      throw new IllegalArgumentException("player_resource condition requires numeric 'value'");
    }

    return conditionFactory.playerResource(resourceType, value, operator);
  }

  private Condition parsePotionEffect(ConditionConfig config) {
    String effectStr = config.effect();
    if (effectStr == null || effectStr.isBlank()) {
      throw new IllegalArgumentException("potion_effect condition requires 'effect'");
    }

    // String key identity: no live potion registry required at parse time
    if (config.amplifier() != null && config.operator() != null) {
      int amplifier = config.amplifier();
      RelationalOperator operator = parseRelationalOperator(config.operator());
      PotionConditionType conditionType = PotionConditionType.AMPLIFIER;
      return conditionFactory.potion(effectStr, amplifier, conditionType, operator);
    }

    // Otherwise just check if effect is present
    return conditionFactory.potionType(effectStr);
  }

  private Condition parseLiquid(ConditionConfig config) {
    Boolean touching = config.touching();
    if (touching == null || !touching) {
      throw new IllegalArgumentException("liquid condition currently only supports touching=true");
    }
    // Prefer explicit material from value when present; default water
    String materialKey = "water";
    if (config.value() instanceof String raw && !raw.isBlank()) {
      materialKey = raw;
    }
    return conditionFactory.liquid(materialKey);
  }

  private Condition parseWeather(ConditionConfig config) {
    String weatherStr = (String) config.value();
    WeatherState state = WeatherState.valueOf(weatherStr.toUpperCase());
    return conditionFactory.weather(state);
  }

  private Condition parseJob(ConditionConfig config) {
    // Single job key
    if (config.value() != null) {
      String jobKey = (String) config.value();
      String namespacedKey = namespaceJobKey(jobKey);
      return conditionFactory.job(namespacedKey);
    }

    // Multiple job keys (any match)
    if (config.values() != null) {
      String[] jobKeys = config.values().stream()
          .map(String::valueOf)
          .map(this::namespaceJobKey)
          .toArray(String[]::new);
      return conditionFactory.jobAny(jobKeys);
    }

    throw new IllegalArgumentException("job condition requires 'value' or 'values'");
  }

  /**
   * Ensure job key is properly namespaced.
   * If the key already contains a colon, return as-is.
   * Otherwise, prepend "modularjobs:" namespace.
   */
  private String namespaceJobKey(String jobKey) {
    if (jobKey.contains(":")) {
      return jobKey;
    }
    return "modularjobs:" + jobKey;
  }

  private Condition parseComposite(ConditionConfig config, LogicalOperator operator) {
    List<ConditionConfig> subConditions = config.conditions();
    if (subConditions == null || subConditions.size() < 2) {
      throw new IllegalArgumentException(
          "Composite condition '" + config.type() + "' requires at least 2 conditions"
      );
    }

    List<Condition> parsed = new ArrayList<>();
    for (ConditionConfig subConfig : subConditions) {
      parsed.add(parse(subConfig));
    }

    // Chain conditions with the operator
    Condition result = parsed.get(0);
    for (int i = 1; i < parsed.size(); i++) {
      result = conditionFactory.compose(result, parsed.get(i), operator);
    }
    return result;
  }

  private Condition parseNegation(ConditionConfig config) {
    ConditionConfig subCondition = config.condition();
    if (subCondition == null) {
      throw new IllegalArgumentException("not condition requires 'condition'");
    }
    return conditionFactory.negate(parse(subCondition));
  }

  private RelationalOperator parseRelationalOperator(String operator) {
    return switch (operator.toLowerCase()) {
      case "less_than", "<" -> RelationalOperator.LESS_THAN;
      case "less_than_or_equal", "<=" -> RelationalOperator.LESS_THAN_OR_EQUAL;
      case "greater_than", ">" -> RelationalOperator.GREATER_THAN;
      case "greater_than_or_equal", ">=" -> RelationalOperator.GREATER_THAN_OR_EQUAL;
      case "equal", "==" -> RelationalOperator.EQUAL;
      case "not_equal", "!=" -> RelationalOperator.NOT_EQUAL;
      default -> throw new IllegalArgumentException("Unknown operator: " + operator);
    };
  }

  private PlayerResourceType parsePlayerResourceType(String resourceTypeStr) {
    return switch (resourceTypeStr.toUpperCase()) {
      case "HEALTH", "HP" -> PlayerResourceType.HEALTH;
      case "HUNGER", "FOOD", "FOOD_LEVEL" -> PlayerResourceType.HUNGER;
      case "EXPERIENCE", "XP", "EXP" -> PlayerResourceType.EXPERIENCE;
      default -> throw new IllegalArgumentException(
          "Unknown player resource type: " + resourceTypeStr
              + " (expected health, hunger/food_level, experience)");
    };
  }

}
