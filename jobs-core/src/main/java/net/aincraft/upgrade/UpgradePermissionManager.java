package net.aincraft.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

/**
 * Manages temporary permission grants for upgrade nodes via Bukkit PermissionAttachment.
 * Uses a single PermissionAttachment per player for efficiency.
 */
@Singleton
public final class UpgradePermissionManager {
  private final Plugin plugin;
  private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

  @Inject
  public UpgradePermissionManager(Plugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Grant a permission to a player.
   * Creates a PermissionAttachment if needed.
   */
  public void grantPermission(Player player, String permission) {
    PermissionAttachment attachment = attachments.computeIfAbsent(
        player.getUniqueId(),
        uuid -> player.addAttachment(plugin)
    );
    attachment.setPermission(permission, true);
  }

  /**
   * Revoke a permission from a player.
   */
  public void revokePermission(Player player, String permission) {
    PermissionAttachment attachment = attachments.get(player.getUniqueId());
    if (attachment != null) {
      attachment.unsetPermission(permission);
    }
  }

  /**
   * Revoke all specified permissions from a player.
   */
  public void revokeAllPermissions(Player player, Set<String> permissions) {
    for (String permission : permissions) {
      revokePermission(player, permission);
    }
  }

  /**
   * Cleanup all permissions for a player (call on logout).
   * Removes the PermissionAttachment entirely.
   */
  public void cleanupPlayer(UUID playerId) {
    PermissionAttachment attachment = attachments.remove(playerId);
    if (attachment != null) {
      attachment.remove();
    }
  }
}
