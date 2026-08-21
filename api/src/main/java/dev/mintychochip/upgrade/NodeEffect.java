package dev.mintychochip.upgrade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import dev.mintychochip.container.BoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Effect granted by a skill level or major node. Sealed vocabulary; unknown
 * types fail tree loading rather than silently no-op.
 */
public sealed interface NodeEffect permits
    NodeEffect.BoostEffect,
    NodeEffect.RuledBoostEffect,
    NodeEffect.PermissionEffect,
    NodeEffect.CapabilityEffect,
    NodeEffect.RecipeUnlockEffect,
    NodeEffect.StateSetEffect {

  /** Simple multiplier boost for a payable target. */
  record BoostEffect(@NotNull String target, @NotNull BigDecimal multiplier) implements NodeEffect {
    public static final String TARGET_XP = "xp";
    public static final String TARGET_MONEY = "money";
    public static final String TARGET_ALL = "all";

    public static BoostEffect of(String target, double multiplier) {
      return new BoostEffect(target, BigDecimal.valueOf(multiplier));
    }
  }

  /** Full BoostSource effect with rules/conditions, reusing the composition API. */
  record RuledBoostEffect(@NotNull String target, @NotNull BoostSource boostSource) implements NodeEffect {
  }

  /** One or more temporary permissions granted via PermissionAttachment. */
  record PermissionEffect(@NotNull List<String> permissions) implements NodeEffect {
    public PermissionEffect(String permission) {
      this(List.of(permission));
    }
  }

  /** Unlocks a namespaced crafting/smelting recipe. */
  record RecipeUnlockEffect(@NotNull Key recipeKey) implements NodeEffect {
  }

  /** Grants a versioned capability payload for later handler dispatch. */
  record CapabilityEffect(@NotNull Key key, int schema, @NotNull Map<String, String> payload)
      implements NodeEffect {
    public CapabilityEffect {
      if (schema <= 0) {
        throw new IllegalArgumentException("Capability schema must be positive: " + schema);
      }
      payload = Map.copyOf(payload);
    }
  }

  /** Sets or removes a namespaced tree-state key. */
  record StateSetEffect(@NotNull Key key, @NotNull String value, boolean remove) implements NodeEffect {
  }

  static BoostEffect boost(String target, double multiplier) {
    return BoostEffect.of(target, multiplier);
  }
}
