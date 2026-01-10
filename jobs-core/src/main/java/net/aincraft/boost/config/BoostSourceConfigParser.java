package net.aincraft.boost.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.config.BoostSourceConfig.BoostConfig;
import net.aincraft.boost.config.BoostSourceConfig.RuleConfig;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.kyori.adventure.key.Key;

/**
 * Parses BoostSourceConfig from JSON into BoostSource instances.
 * Provides reusable parsing methods for use across different config contexts.
 */
public final class BoostSourceConfigParser {

  private final ConditionFactory conditionFactory;
  private final BoostFactory boostFactory;
  private final ConditionConfigParser conditionParser;

  public BoostSourceConfigParser(
      ConditionFactory conditionFactory,
      BoostFactory boostFactory
  ) {
    this.conditionFactory = conditionFactory;
    this.boostFactory = boostFactory;
    this.conditionParser = new ConditionConfigParser(conditionFactory);
  }

  /**
   * Parse a complete BoostSourceConfig into a BoostSource.
   */
  public BoostSource parse(BoostSourceConfig config) {
    Key key = Key.key(config.key());
    List<Rule> rules = new ArrayList<>();

    for (RuleConfig ruleConfig : config.rules()) {
      Rule rule = parseRule(ruleConfig);
      rules.add(rule);
    }

    return new RuledBoostSourceImpl(rules, key, config.description());
  }

  /**
   * Build a BoostSource from individual components.
   * Useful when constructing from non-standard config formats.
   */
  public BoostSource buildBoostSource(
      Key key,
      String description,
      List<RuleConfig> ruleConfigs
  ) {
    List<Rule> rules = new ArrayList<>();

    if (ruleConfigs != null) {
      for (RuleConfig ruleConfig : ruleConfigs) {
        Rule rule = parseRule(ruleConfig);
        rules.add(rule);
      }
    }

    return new RuledBoostSourceImpl(rules, key, description);
  }

  /**
   * Parse a rule configuration.
   */
  public Rule parseRule(RuleConfig config) {
    Condition condition = conditionParser.parse(config.conditions());
    Boost boost = parseBoost(config.boost());
    int priority = config.priority();
    return new Rule(condition, priority, boost);
  }

  /**
   * Parse a boost configuration.
   */
  public Boost parseBoost(BoostConfig config) {
    BigDecimal amount = BigDecimal.valueOf(config.amount());
    return switch (config.type().toLowerCase()) {
      case "multiplicative" -> boostFactory.multiplicative(amount);
      case "additive" -> boostFactory.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + config.type());
    };
  }

  // Overloaded methods for UpgradeTreeConfig types

  /**
   * Parse an UpgradeTreeConfig rule configuration.
   */
  public Rule parseRule(net.aincraft.upgrade.config.UpgradeTreeConfig.RuleConfig config) {
    Condition condition = conditionParser.parse(config.conditions());
    Boost boost = parseBoost(config.boost());
    int priority = config.priority();
    return new Rule(condition, priority, boost);
  }

  /**
   * Parse an UpgradeTreeConfig boost configuration.
   */
  public Boost parseBoost(net.aincraft.upgrade.config.UpgradeTreeConfig.BoostConfig config) {
    BigDecimal amount = BigDecimal.valueOf(config.amount());
    return switch (config.type().toLowerCase()) {
      case "multiplicative" -> boostFactory.multiplicative(amount);
      case "additive" -> boostFactory.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + config.type());
    };
  }
}
