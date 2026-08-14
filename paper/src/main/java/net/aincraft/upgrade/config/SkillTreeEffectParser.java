package net.aincraft.upgrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.boost.config.BoostSourceConfig;
import net.aincraft.boost.config.BoostSourceConfigParser;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.upgrade.NodeEffect;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the JSON effect vocabulary into sealed NodeEffect instances.
 * Unknown types throw so misconfigurations are loud.
 */
public final class SkillTreeEffectParser {

  private final BoostSourceConfigParser boostSourceParser;

  public SkillTreeEffectParser(BoostFactory boostFactory, ConditionFactory conditionFactory) {
    this.boostSourceParser = new BoostSourceConfigParser(conditionFactory, boostFactory);
  }

  public NodeEffect parse(@NotNull JsonElement element) {
    JsonObject obj = element.getAsJsonObject();
    String type = obj.get("type").getAsString();

    return switch (type) {
      case "boost" -> new NodeEffect.BoostEffect(
          obj.has("target") ? obj.get("target").getAsString() : NodeEffect.BoostEffect.TARGET_ALL,
          obj.has("amount") ? BigDecimal.valueOf(obj.get("amount").getAsDouble()) : BigDecimal.ONE);
      case "ruled_boost" -> parseRuledBoost(obj);
      case "permission" -> new NodeEffect.PermissionEffect(obj.get("key").getAsString());
      case "recipe_unlock" ->
          new NodeEffect.RecipeUnlockEffect(Key.key(obj.get("recipe").getAsString()));
      case "capability" -> parseCapability(obj);
      case "state_set" -> new NodeEffect.StateSetEffect(
          parseKey(obj.get("key").getAsString()),
          obj.get("value").getAsString(),
          obj.has("remove") && obj.get("remove").getAsBoolean());
      default -> throw new IllegalArgumentException("Unknown effect type: " + type);
    };
  }

  private NodeEffect parseRuledBoost(JsonObject obj) {
    String target = obj.has("target")
        ? obj.get("target").getAsString()
        : NodeEffect.BoostEffect.TARGET_ALL;
    BoostSourceConfig config = new Gson().fromJson(obj, BoostSourceConfig.class);
    return new NodeEffect.RuledBoostEffect(target, boostSourceParser.parse(config));
  }

  private NodeEffect parseCapability(JsonObject obj) {
    String rawCapability = obj.get("capability").getAsString();
    int schemaVersion = obj.get("schema").getAsInt();
    if (schemaVersion <= 0 || !rawCapability.contains(":")) {
      throw new IllegalArgumentException("Capability requires namespaced key and positive schema");
    }
    JsonObject payloadObject = obj.get("payload").getAsJsonObject();
    Map<String, String> payload = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : payloadObject.entrySet()) {
      if (!entry.getValue().isJsonPrimitive()
          || !entry.getValue().getAsJsonPrimitive().isString()) {
        throw new IllegalArgumentException("Capability payload values must be strings");
      }
      payload.put(entry.getKey(), entry.getValue().getAsString());
    }
    return new NodeEffect.CapabilityEffect(parseKey(rawCapability), schemaVersion, payload);
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
