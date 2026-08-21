package dev.mintychochip.hooks;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Grants/revokes job perk permissions (LuckPerms when present, else Bukkit attachments).
 *
 */
public final class PermissionHook {

  private final Plugin plugin;
  private final Logger logger;

  public PermissionHook(Plugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
  }

  public void grantPerkPermission(@NotNull Player player, @NotNull String perkConfigName) {
    grantPermission(player, perkPermission(perkConfigName));
  }

  public void revokePerkPermission(@NotNull Player player, @NotNull String perkConfigName) {
    revokePermission(player, perkPermission(perkConfigName));
  }

  public void grantPermission(@NotNull Player player, @NotNull String permission) {
    if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
      grantViaLuckPerms(player, permission);
      return;
    }
    PermissionAttachment attachment = player.addAttachment(plugin);
    attachment.setPermission(permission, true);
  }

  public void revokePermission(@NotNull Player player, @NotNull String permission) {
    if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
      revokeViaLuckPerms(player, permission);
      return;
    }
    player.addAttachment(plugin).setPermission(permission, false);
  }

  private static String perkPermission(String perkConfigName) {
    if (perkConfigName.startsWith("storage.")) {
      return "aincraft-mining." + perkConfigName.toLowerCase(java.util.Locale.ROOT);
    }
    return "aincraft-mining.perk." + perkConfigName.toLowerCase(java.util.Locale.ROOT);
  }

  private void grantViaLuckPerms(Player player, String permission) {
    try {
      Class<?> lpClass = Class.forName("net.luckperms.api.LuckPermsProvider");
      Object lp = lpClass.getMethod("get").invoke(null);
      Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
      Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
          .invoke(userManager, player.getUniqueId());
      if (user != null) {
        Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
        Object node = nodeClass.getMethod("builder", String.class).invoke(null, permission);
        node = node.getClass().getMethod("build").invoke(node);
        Object data = user.getClass().getMethod("data").invoke(user);
        data.getClass().getMethod("add", nodeClass).invoke(data, node);
        userManager.getClass().getMethod("saveUser", user.getClass()).invoke(userManager, user);
      }
    } catch (ReflectiveOperationException e) {
      logger.warning("Failed to grant permission via LuckPerms: " + e.getMessage());
      player.addAttachment(plugin).setPermission(permission, true);
    }
  }

  private void revokeViaLuckPerms(Player player, String permission) {
    try {
      Class<?> lpClass = Class.forName("net.luckperms.api.LuckPermsProvider");
      Object lp = lpClass.getMethod("get").invoke(null);
      Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
      Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
          .invoke(userManager, player.getUniqueId());
      if (user != null) {
        Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
        Object node = nodeClass.getMethod("builder", String.class).invoke(null, permission);
        node = node.getClass().getMethod("build").invoke(node);
        Object data = user.getClass().getMethod("data").invoke(user);
        data.getClass().getMethod("remove", nodeClass).invoke(data, node);
        userManager.getClass().getMethod("saveUser", user.getClass()).invoke(userManager, user);
      }
    } catch (ReflectiveOperationException e) {
      logger.warning("Failed to revoke permission via LuckPerms: " + e.getMessage());
    }
  }
}
