package dev.mintychochip.boost;

import java.util.ArrayList;
import java.util.List;
import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.databag.AllOfCondition;
import dev.mintychochip.databag.AlwaysCondition;
import dev.mintychochip.databag.AnyOfCondition;
import dev.mintychochip.databag.BabyCondition;
import dev.mintychochip.databag.BiomeCondition;
import dev.mintychochip.databag.BlockIdCondition;
import dev.mintychochip.databag.BlockPropertyCondition;
import dev.mintychochip.databag.EntityTypeCondition;
import dev.mintychochip.databag.FluidCondition;
import dev.mintychochip.databag.FlyingCondition;
import dev.mintychochip.databag.GameModeCondition;
import dev.mintychochip.databag.GlidingCondition;
import dev.mintychochip.databag.InvertedCondition;
import dev.mintychochip.databag.JobCondition;
import dev.mintychochip.databag.OnFireCondition;
import dev.mintychochip.databag.OnGroundCondition;
import dev.mintychochip.databag.PlayerResourceCondition;
import dev.mintychochip.databag.PotionAmplifierCondition;
import dev.mintychochip.databag.PotionDurationCondition;
import dev.mintychochip.databag.PotionPresentCondition;
import dev.mintychochip.databag.SneakingCondition;
import dev.mintychochip.databag.SprintingCondition;
import dev.mintychochip.databag.SwimmingCondition;
import dev.mintychochip.databag.WeatherCondition;
import dev.mintychochip.databag.WorldCondition;
import dev.mintychochip.container.boost.Condition;

/**
 * Formats a boost condition tree for admin command output.
 */
public final class ConditionTreeFormatter {

  private ConditionTreeFormatter() {}

  public static List<String> format(Condition condition, String indent) {
    List<String> lines = new ArrayList<>();
    formatBoost(condition, indent, "", true, lines);
    return lines;
  }

  private static void formatBoost(
      Condition condition, String baseIndent, String prefix, boolean isLast, List<String> lines) {
    if (condition instanceof SnapshotCondition snapshot) {
      formatApi(snapshot.delegate(), baseIndent, prefix, isLast, lines);
      return;
    }
    String connector = isLast ? "└── " : "├── ";
    lines.add(baseIndent + prefix + connector
        + condition.getClass().getSimpleName().replace("Impl", "").replace("Condition", ""));
  }

  private static void formatApi(
      dev.mintychochip.databag.Condition condition,
      String baseIndent,
      String prefix,
      boolean isLast,
      List<String> lines) {
    String connector = isLast ? "└── " : "├── ";
    String childPrefix = isLast ? "    " : "│   ";
    switch (condition) {
      case AlwaysCondition ignored ->
          lines.add(baseIndent + prefix + connector + "Always");
      case AllOfCondition all -> {
        lines.add(baseIndent + prefix + connector + "AND");
        for (int i = 0; i < all.terms().size(); i++) {
          formatApi(all.terms().get(i), baseIndent, prefix + childPrefix,
              i == all.terms().size() - 1, lines);
        }
      }
      case AnyOfCondition any -> {
        lines.add(baseIndent + prefix + connector + "OR");
        for (int i = 0; i < any.terms().size(); i++) {
          formatApi(any.terms().get(i), baseIndent, prefix + childPrefix,
              i == any.terms().size() - 1, lines);
        }
      }
      case InvertedCondition inverted -> {
        lines.add(baseIndent + prefix + connector + "NOT");
        formatApi(inverted.term(), baseIndent, prefix + childPrefix, true, lines);
      }
      case SneakingCondition sneak ->
          lines.add(baseIndent + prefix + connector + "Sneaking: " + sneak.expected());
      case SprintingCondition sprint ->
          lines.add(baseIndent + prefix + connector + "Sprinting: " + sprint.expected());
      case EntityTypeCondition type ->
          lines.add(baseIndent + prefix + connector + "Entity: " + type.entityType().asString());
      case OnFireCondition fire ->
          lines.add(baseIndent + prefix + connector + "On fire: " + fire.expected());
      case OnGroundCondition ground ->
          lines.add(baseIndent + prefix + connector + "On ground: " + ground.expected());
      case SwimmingCondition swim ->
          lines.add(baseIndent + prefix + connector + "Swimming: " + swim.expected());
      case BabyCondition baby ->
          lines.add(baseIndent + prefix + connector + "Baby: " + baby.expected());
      case GlidingCondition glide ->
          lines.add(baseIndent + prefix + connector + "Gliding: " + glide.expected());
      case FlyingCondition fly ->
          lines.add(baseIndent + prefix + connector + "Flying: " + fly.expected());
      case GameModeCondition mode ->
          lines.add(baseIndent + prefix + connector + "Game mode: " + mode.gameMode());
      case BlockIdCondition block ->
          lines.add(baseIndent + prefix + connector + "Block: " + block.blockId().asString());
      case BlockPropertyCondition prop ->
          lines.add(baseIndent + prefix + connector + "Block " + prop.name() + "=" + prop.value());
      case BiomeCondition biome ->
          lines.add(baseIndent + prefix + connector + "Biome: " + biome.biomeKey().value());
      case WorldCondition world ->
          lines.add(baseIndent + prefix + connector + "World: " + world.worldName());
      case WeatherCondition weather ->
          lines.add(baseIndent + prefix + connector + "Weather: " + weather.state());
      case FluidCondition fluid ->
          lines.add(baseIndent + prefix + connector + "In Liquid: " + fluid.fluidKey().value());
      case PlayerResourceCondition resource ->
          lines.add(baseIndent + prefix + connector + resource.type() + " "
              + resource.operator() + " " + resource.expected());
      case PotionPresentCondition potion ->
          lines.add(baseIndent + prefix + connector + "Has Potion: " + potion.effectKey().value());
      case PotionAmplifierCondition potion ->
          lines.add(baseIndent + prefix + connector + "Potion: " + potion.effectKey().value()
              + " amplifier " + potion.operator() + " " + potion.expected());
      case PotionDurationCondition potion ->
          lines.add(baseIndent + prefix + connector + "Potion: " + potion.effectKey().value()
              + " duration " + potion.operator() + " " + potion.expected());
      case JobCondition job ->
          lines.add(baseIndent + prefix + connector + "Job: " + job.jobKeys());
      default -> lines.add(baseIndent + prefix + connector + condition.getClass().getSimpleName());
    }
  }
}
