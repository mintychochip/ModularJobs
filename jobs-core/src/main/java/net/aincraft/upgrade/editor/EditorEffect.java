package net.aincraft.upgrade.editor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.upgrade.UpgradeEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable effect data for editing purposes.
 * Supports all effect types: boost, passive, permission, ruled_boost.
 */
public final class EditorEffect {

  public enum EffectType {
    BOOST,
    PASSIVE,
    PERMISSION,
    RULED_BOOST
  }

  private EffectType type;

  // Boost fields
  private String target;       // "xp", "money"
  private double amount;       // multiplier (e.g., 1.1 for 10%)

  // Passive fields
  private String ability;      // ability identifier
  private String passiveDescription;

  // Permission fields
  private String permission;
  private List<String> permissions;

  // Ruled boost fields (simplified for editor)
  private String ruledDescription;
  // Full ruled boost config stored as JSON string for now
  private String ruledConfigJson;

  public EditorEffect() {
    this.type = EffectType.BOOST;
    this.target = "xp";
    this.amount = 1.1;
    this.permissions = new ArrayList<>();
  }

  /**
   * Create from an existing UpgradeEffect.
   */
  public static EditorEffect fromUpgradeEffect(@NotNull UpgradeEffect source) {
    EditorEffect effect = new EditorEffect();

    switch (source) {
      case UpgradeEffect.BoostEffect boost -> {
        effect.type = EffectType.BOOST;
        effect.target = boost.target();
        effect.amount = boost.multiplier().doubleValue();
      }
      case UpgradeEffect.PermissionEffect perm -> {
        effect.type = EffectType.PERMISSION;
        effect.permissions = new ArrayList<>(perm.permissions());
        effect.permission = perm.permission(); // For backward compatibility
      }
      case UpgradeEffect.RuledBoostEffect ruled -> {
        effect.type = EffectType.RULED_BOOST;
        effect.target = ruled.target();
        effect.ruledDescription = ruled.boostSource().description();
        // Store as simplified representation for now
        effect.ruledConfigJson = "{}"; // TODO: Serialize full config
      }
    }

    return effect;
  }

  // ========== Getters/Setters ==========

  public EffectType type() { return type; }
  public void setType(EffectType type) { this.type = type; }

  public String target() { return target; }
  public void setTarget(String target) { this.target = target; }

  public double amount() { return amount; }
  public void setAmount(double amount) { this.amount = amount; }

  public String ability() { return ability; }
  public void setAbility(String ability) { this.ability = ability; }

  public String passiveDescription() { return passiveDescription; }
  public void setPassiveDescription(String desc) { this.passiveDescription = desc; }

  public String permission() { return permission; }
  public void setPermission(String permission) { this.permission = permission; }

  public List<String> permissions() { return permissions; }
  public void setPermissions(List<String> permissions) { this.permissions = permissions != null ? permissions : new ArrayList<>(); }

  public String ruledDescription() { return ruledDescription; }
  public void setRuledDescription(String desc) { this.ruledDescription = desc; }

  public String ruledConfigJson() { return ruledConfigJson; }
  public void setRuledConfigJson(String json) { this.ruledConfigJson = json; }

  // ========== Display Helpers ==========

  /**
   * Get a human-readable description of this effect.
   */
  public String getDisplayDescription() {
    return switch (type) {
      case BOOST -> String.format("+%.0f%% %s", (amount - 1) * 100, target);
      case PASSIVE -> ability + (passiveDescription != null ? ": " + passiveDescription : "");
      case PERMISSION -> {
        if (permissions != null && !permissions.isEmpty()) {
          if (permissions.size() == 1) {
            yield "Permission: " + permissions.get(0);
          } else {
            yield "Permissions: " + String.join(", ", permissions);
          }
        } else {
          yield "Permission: " + permission;
        }
      }
      case RULED_BOOST -> ruledDescription != null ? ruledDescription : "Conditional boost";
    };
  }

  /**
   * Create a deep copy of this effect.
   */
  public EditorEffect copy() {
    EditorEffect copy = new EditorEffect();
    copy.type = this.type;
    copy.target = this.target;
    copy.amount = this.amount;
    copy.ability = this.ability;
    copy.passiveDescription = this.passiveDescription;
    copy.permission = this.permission;
    copy.permissions = new ArrayList<>(this.permissions);
    copy.ruledDescription = this.ruledDescription;
    copy.ruledConfigJson = this.ruledConfigJson;
    return copy;
  }
}
