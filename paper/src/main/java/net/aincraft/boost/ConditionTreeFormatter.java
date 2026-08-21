package net.aincraft.boost;

import java.util.ArrayList;
import java.util.List;
import net.aincraft.boost.conditions.SnapshotCondition;
import dev.conditions.AllOfCondition;
import dev.conditions.AlwaysCondition;
import dev.conditions.AnyOfCondition;
import dev.conditions.BabyCondition;
import dev.conditions.BiomeCondition;
import dev.conditions.BlockIdCondition;
import dev.conditions.BlockPropertyCondition;
import dev.conditions.EntityTypeCondition;
import dev.conditions.FluidCondition;
import dev.conditions.FlyingCondition;
import dev.conditions.GameModeCondition;
import dev.conditions.GlidingCondition;
import dev.conditions.InvertedCondition;
import dev.conditions.JobCondition;
import dev.conditions.OnFireCondition;
import dev.conditions.OnGroundCondition;
import dev.conditions.PlayerResourceCondition;
import dev.conditions.PotionAmplifierCondition;
import dev.conditions.PotionDurationCondition;
import dev.conditions.PotionPresentCondition;
import dev.conditions.SneakingCondition;
import dev.conditions.SprintingCondition;
import dev.conditions.SwimmingCondition;
import dev.conditions.WeatherCondition;
import dev.conditions.WorldCondition;
import net.aincraft.container.boost.Condition;

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
      dev.conditions.Condition condition,
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
