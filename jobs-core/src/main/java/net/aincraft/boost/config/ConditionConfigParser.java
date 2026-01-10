package net.aincraft.boost.config;

import java.util.ArrayList;
import java.util.List;
import net.aincraft.boost.config.BoostSourceConfig.ConditionConfig;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import net.aincraft.container.boost.factories.ConditionFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;

/**
 * Parses ConditionConfig from JSON into Condition instances.
 */
public final class ConditionConfigParser {

  private final ConditionFactory conditionFactory;

  public ConditionConfigParser(ConditionFactory conditionFactory) {
    this.conditionFactory = conditionFactory;
  }

  public Condition parse(ConditionConfig config) {
    return switch (config.type().toLowerCase()) {
      case "always" -> new AlwaysTrueCondition();
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
    String biomeStr = (String) config.value();
    Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(biomeStr.toLowerCase()));
    if (biome == null) {
      throw new IllegalArgumentException("Unknown biome: " + biomeStr);
    }
    return conditionFactory.biome(biome);
  }

  private Condition parseWorld(ConditionConfig config) {
    String worldName = (String) config.value();
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      throw new IllegalArgumentException("World not found: " + worldName);
    }
    return conditionFactory.world(world);
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
    PlayerResourceType resourceType = PlayerResourceType.valueOf(resourceTypeStr.toUpperCase());

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
    if (effectStr == null) {
      throw new IllegalArgumentException("potion_effect condition requires 'effect'");
    }

    PotionEffectType effectType = PotionEffectType.getByName(effectStr.toUpperCase());
    if (effectType == null) {
      throw new IllegalArgumentException("Unknown potion effect: " + effectStr);
    }

    // If amplifier and operator specified, use full potion condition
    if (config.amplifier() != null && config.operator() != null) {
      int amplifier = config.amplifier();
      RelationalOperator operator = parseRelationalOperator(config.operator());
      PotionConditionType conditionType = PotionConditionType.AMPLIFIER;
      return conditionFactory.potion(effectType, amplifier, conditionType, operator);
    }

    // Otherwise just check if effect is present
    return conditionFactory.potionType(effectType);
  }

  private Condition parseLiquid(ConditionConfig config) {
    Boolean touching = config.touching();
    if (touching == null || !touching) {
      throw new IllegalArgumentException("liquid condition currently only supports touching=true");
    }
    // Use water as default liquid material
    return conditionFactory.liquid(Material.WATER);
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

  /**
   * Always-true condition for base rules without conditions.
   */
  private static class AlwaysTrueCondition implements Condition {
    @Override
    public boolean applies(net.aincraft.container.BoostContext context) {
      return true;
    }
  }
}
