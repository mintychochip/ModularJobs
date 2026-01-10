package net.aincraft.boost.config;

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

  public record RuleConfig(
      int priority,
      @NotNull ConditionConfig conditions,
      @NotNull BoostConfig boost
  ) {
  }

  public record BoostConfig(
      @NotNull String type,
      double amount
  ) {
  }

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
