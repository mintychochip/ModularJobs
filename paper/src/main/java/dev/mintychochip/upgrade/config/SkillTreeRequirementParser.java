package dev.mintychochip.upgrade.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.mintychochip.upgrade.Requirement;
import dev.mintychochip.upgrade.Requirements;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the JSON requirement vocabulary into the sealed Requirement tree. Unknown types throw so
 * misconfigurations are loud.
 */
public final class SkillTreeRequirementParser {

  /** API member. */
  @NotNull
  public Requirement parse(@NotNull JsonElement element) {
    JsonObject obj = element.getAsJsonObject();
    String type =
        obj.has("type")
            ? obj.get("type").getAsString()
            : obj.has("all") ? "all" : obj.has("any") ? "any" : obj.has("not") ? "not" : "";

    return switch (type) {
      case "all" ->
          new Requirements.AllOf(
              parseList(
                  obj.has("requirements")
                      ? obj.getAsJsonArray("requirements")
                      : obj.getAsJsonArray("all")));
      case "any" ->
          new Requirements.AnyOf(
              parseList(
                  obj.has("requirements")
                      ? obj.getAsJsonArray("requirements")
                      : obj.getAsJsonArray("any")));
      case "not" ->
          new Requirements.Not(
              parse(obj.has("requirement") ? obj.get("requirement") : obj.get("not")));
      case "job_level" -> new Requirements.JobLevelRequirement(obj.get("minimum").getAsInt());
      case "node_level" ->
          new Requirements.NodeLevelRequirement(
              obj.get("node").getAsString(), obj.get("minimum").getAsInt());
      case "node_unlocked" ->
          new Requirements.NodeUnlockedRequirement(obj.get("node").getAsString());
      case "state_equals" ->
          new Requirements.StateEqualsRequirement(
              parseKey(obj.get("key").getAsString()), obj.get("value").getAsString());
      case "permission" -> new Requirements.PermissionRequirement(obj.get("key").getAsString());
      default -> throw new IllegalArgumentException("Unknown requirement type: " + type);
    };
  }

  private List<Requirement> parseList(JsonArray array) {
    List<Requirement> result = new ArrayList<>();
    if (array != null) {
      for (JsonElement el : array) {
        result.add(parse(el));
      }
    }
    return List.copyOf(result);
  }

  private Key parseKey(String raw) {
    int separator = raw.indexOf(':');
    if (separator > 0) {
      return Key.key(raw.substring(0, separator), raw.substring(separator + 1));
    }
    int dot = raw.indexOf('.');
    if (dot > 0) {
      return Key.key(raw.substring(0, dot), raw.substring(dot + 1));
    }
    throw new IllegalArgumentException(
        "Namespaced key must use namespace.key or namespace:key: " + raw);
  }
}
