package net.aincraft.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import org.bukkit.entity.Player;

/**
 * Applies and unapplies upgrade effects when nodes are unlocked or reset.
 */
@Singleton
public final class UpgradeEffectApplier {
  private final UpgradePermissionManager permissionManager;

  @Inject
  public UpgradeEffectApplier(UpgradePermissionManager permissionManager) {
    this.permissionManager = permissionManager;
  }

  /**
   * Apply all effects from an upgrade node when it's unlocked.
   */
  public void applyNodeEffects(Player player, UpgradeNode node) {
    for (UpgradeEffect effect : node.effects()) {
      if (effect instanceof UpgradeEffect.PermissionEffect perm) {
        for (String permission : perm.permissions()) {
          permissionManager.grantPermission(player, permission);
        }
      }
      // Future: handle other effect types (BoostEffect, PassiveEffect, StatEffect)
    }
  }

  /**
   * Unapply all effects from an upgrade node when it's locked (tree reset).
   */
  public void unapplyNodeEffects(Player player, UpgradeNode node) {
    for (UpgradeEffect effect : node.effects()) {
      if (effect instanceof UpgradeEffect.PermissionEffect perm) {
        for (String permission : perm.permissions()) {
          permissionManager.revokePermission(player, permission);
        }
      }
      // Future: handle other effect types
    }
  }

  /**
   * Restore all effects from unlocked nodes on player login.
   * Respects perk policies - for MAX policy perks, only the highest level is applied.
   */
  public void restoreEffects(Player player, UpgradeTree tree, Set<String> unlockedNodeKeys) {
    // Filter nodes based on perk policies
    java.util.Map<String, UpgradeNode> activeNodes = new java.util.HashMap<>();

    for (String nodeKey : unlockedNodeKeys) {
      var nodeOpt = tree.getNode(nodeKey);
      if (nodeOpt.isEmpty()) {
        continue;
      }
      UpgradeNode node = nodeOpt.get();

      // This is a perk node - check its policy
      PerkPolicy policy = tree.getPerkPolicy(node.perkId());

      if (policy == PerkPolicy.MAX) {
        // Only keep the highest level
        UpgradeNode existing = activeNodes.get(node.perkId());
        if (existing == null || node.level() > existing.level()) {
          activeNodes.put(node.perkId(), node);
        }
      } else {
        // ADDITIVE - keep all levels, use node key to avoid conflicts
        activeNodes.put(node.key().asString(), node);
      }
    }

    // Apply effects from filtered nodes
    for (UpgradeNode node : activeNodes.values()) {
      applyNodeEffects(player, node);
    }
  }
}
