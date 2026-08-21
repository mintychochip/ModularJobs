package dev.mintychochip.boost.config;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JSON model for boost source configuration.
 */
public record BoostSourceConfig(
    @NotNull String key,
    @Nullable String description,
    @Nullable PolicyConfig policy,
    @NotNull List<RuleConfig> rules
) {

  public record PolicyConfig(
      @NotNull String type,
      @Nullable Integer k
  ) {
  }

  /**
   * A single conditional boost rule: when {@code conditions} hold, {@code boost} applies;
   * higher {@code priority} wins among matching rules in the same source.
   */
  public record RuleConfig(
      int priority,
      @NotNull ConditionConfig conditions,
      @NotNull BoostConfig boost
  ) {
  }

  /** Type-tagged boost ({@code additive}/{@code multiplicative}) with a fixed amount. */
  public record BoostConfig(
      @NotNull String type,
      double amount
  ) {
  }

  /**
   * Union-shaped condition descriptor: {@code type} selects the condition kind, and the
   * remaining fields carry that kind's parameters (operator, value(s), nested conditions,
   * resource/effect, amplifiers, min/max, touching, etc.).
   */
  public record ConditionConfig(
      @NotNull String type,
      @Nullable String operator,
      @Nullable Object value,
      @Nullable List<Object> values,
      @Nullable List<ConditionConfig> conditions,
      @Nullable ConditionConfig condition,
      @Nullable String resourceType,
      @Nullable String effect,
      @Nullable Integer amplifier,
      @Nullable Long min,
      @Nullable Long max,
      @Nullable Boolean touching
  ) {
  }
}
