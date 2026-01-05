package net.aincraft.boost.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.config.BoostSourceConfig.BoostConfig;
import net.aincraft.boost.config.BoostSourceConfig.PolicyConfig;
import net.aincraft.boost.config.BoostSourceConfig.RuleConfig;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.kyori.adventure.key.Key;

/**
 * Parses BoostSourceConfig from JSON into BoostSource instances.
 */
public final class BoostSourceConfigParser {

  private final ConditionFactory conditionFactory;
  private final PolicyFactory policyFactory;
  private final BoostFactory boostFactory;
  private final ConditionConfigParser conditionParser;

  public BoostSourceConfigParser(
      ConditionFactory conditionFactory,
      PolicyFactory policyFactory,
      BoostFactory boostFactory
  ) {
    this.conditionFactory = conditionFactory;
    this.policyFactory = policyFactory;
    this.boostFactory = boostFactory;
    this.conditionParser = new ConditionConfigParser(conditionFactory);
  }

  public BoostSource parse(BoostSourceConfig config) {
    Key key = Key.key(config.key());
    Policy policy = parsePolicy(config.policy());
    List<Rule> rules = new ArrayList<>();

    for (RuleConfig ruleConfig : config.rules()) {
      Rule rule = parseRule(ruleConfig);
      rules.add(rule);
    }

    return new RuledBoostSourceImpl(rules, policy, key, config.description());
  }

  private Policy parsePolicy(PolicyConfig config) {
    return switch (config.type().toLowerCase()) {
      case "all_applicable" -> policyFactory.allApplicable();
      case "first" -> policyFactory.first();
      case "top_k" -> {
        if (config.k() == null) {
          throw new IllegalArgumentException("top_k policy requires 'k' parameter");
        }
        yield policyFactory.topKBoosts(config.k());
      }
      default -> throw new IllegalArgumentException("Unknown policy type: " + config.type());
    };
  }

  private Rule parseRule(RuleConfig config) {
    Condition condition = conditionParser.parse(config.conditions());
    Boost boost = parseBoost(config.boost());
    int priority = config.priority();
    return new Rule(condition, priority, boost);
  }

  private Boost parseBoost(BoostConfig config) {
    BigDecimal amount = BigDecimal.valueOf(config.amount());
    return switch (config.type().toLowerCase()) {
      case "multiplicative" -> boostFactory.multiplicative(amount);
      case "additive" -> boostFactory.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + config.type());
    };
  }
}
