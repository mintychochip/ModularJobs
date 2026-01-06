package net.aincraft.hooks;

import com.google.inject.Inject;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class JobPetsHook {

  private final Plugin plugin;
  private final Logger logger;

  @Inject
  public JobPetsHook(Plugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
  }

  public boolean isJobPetsAvailable() {
    return Bukkit.getPluginManager().isPluginEnabled("JobPets-Core");
  }

  public void grantPetPermission(@NotNull Player player, @NotNull String petConfigName) {
    String permission = "aincraft-mining.pet." + petConfigName.toLowerCase();
    grantPermission(player, permission);
    logger.info("Granted pet permission " + permission + " to " + player.getName());
  }

  public void grantPerkPermission(@NotNull Player player, @NotNull String perkConfigName) {
    String permission;
    if (perkConfigName.startsWith("storage.")) {
      // storage.tworows -> aincraft-mining.storage.tworows
      permission = "aincraft-mining." + perkConfigName.toLowerCase();
    } else {
      permission = "aincraft-mining.perk." + perkConfigName.toLowerCase();
    }
    grantPermission(player, permission);
    logger.info("Granted permission " + permission + " to " + player.getName());
  }

  public void revokePetPermission(@NotNull Player player, @NotNull String petConfigName) {
    String permission = "aincraft-mining.pet." + petConfigName.toLowerCase();
    revokePermission(player, permission);
  }

  public void revokePerkPermission(@NotNull Player player, @NotNull String perkConfigName) {
    String permission;
    if (perkConfigName.startsWith("storage.")) {
      permission = "aincraft-mining." + perkConfigName.toLowerCase();
    } else {
      permission = "aincraft-mining.perk." + perkConfigName.toLowerCase();
    }
    revokePermission(player, permission);
  }

  public void grantPermission(@NotNull Player player, @NotNull String permission) {
    // Try LuckPerms first if available
    if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
      grantViaLuckPerms(player, permission);
      return;
    }

    // Fallback to Bukkit permission attachment
    PermissionAttachment attachment = player.addAttachment(plugin);
    attachment.setPermission(permission, true);
  }

  public void revokePermission(@NotNull Player player, @NotNull String permission) {
    if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
      revokeViaLuckPerms(player, permission);
      return;
    }

    // For Bukkit attachments, we'd need to track them - simplified version
    player.addAttachment(plugin).setPermission(permission, false);
  }

  private void grantViaLuckPerms(Player player, String permission) {
    try {
      // Use reflection to avoid hard dependency
      Class<?> lpClass = Class.forName("net.luckperms.api.LuckPermsProvider");
      Object lp = lpClass.getMethod("get").invoke(null);
      Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
      Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
          .invoke(userManager, player.getUniqueId());

      if (user != null) {
        // Build permission node
        Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
        Object node = nodeClass.getMethod("builder", String.class)
            .invoke(null, permission);
        node = node.getClass().getMethod("build").invoke(node);

        // Add to user
        Object data = user.getClass().getMethod("data").invoke(user);
        data.getClass().getMethod("add", nodeClass).invoke(data, node);

        // Save
        userManager.getClass().getMethod("saveUser", user.getClass()).invoke(userManager, user);
      }
    } catch (Exception e) {
      logger.warning("Failed to grant permission via LuckPerms: " + e.getMessage());
      // Fallback to Bukkit
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
        Object node = nodeClass.getMethod("builder", String.class)
            .invoke(null, permission);
        node = node.getClass().getMethod("build").invoke(node);

        Object data = user.getClass().getMethod("data").invoke(user);
        data.getClass().getMethod("remove", nodeClass).invoke(data, node);

        userManager.getClass().getMethod("saveUser", user.getClass()).invoke(userManager, user);
      }
    } catch (Exception e) {
      logger.warning("Failed to revoke permission via LuckPerms: " + e.getMessage());
    }
  }

  /**
   * Synchronizes pet selection to jobpets-core's PetService.
   * This ensures the correct pet spawns after login.
   *
   * @param player The player
   * @param petConfigName The pet's config name (e.g., "allay", "bat", "goat")
   * @param jobPlugin The job plugin instance (will try to get from plugin manager if null)
   */
  public void syncPetTypeToJobPets(@NotNull Player player, @NotNull String petConfigName, @org.jetbrains.annotations.Nullable Plugin jobPlugin) {
    if (!isJobPetsAvailable()) {
      logger.warning("Cannot sync pet type - JobPets-Core is not available");
      return;
    }

    try {
      // Get services from ServicesManager
      org.bukkit.plugin.ServicesManager sm = Bukkit.getServicesManager();

      // Get PetService
      org.bukkit.plugin.RegisteredServiceProvider<?> petServiceProvider =
          sm.getRegistration(Class.forName("com.aincraft.jobpets.api.pet.PetService"));
      if (petServiceProvider == null) {
        logger.warning("PetService not registered in ServicesManager");
        return;
      }
      Object petService = petServiceProvider.getProvider();

      // Get PetTypeRegistry
      org.bukkit.plugin.RegisteredServiceProvider<?> registryProvider =
          sm.getRegistration(Class.forName("com.aincraft.jobpets.api.pet.PetTypeRegistry"));
      if (registryProvider == null) {
        logger.warning("PetTypeRegistry not registered in ServicesManager");
        return;
      }
      Object registry = registryProvider.getProvider();

      // Look up PetType by config name
      Class<?> registryClass = registry.getClass();
      Object petType = registryClass.getMethod("getByConfigName", String.class)
          .invoke(registry, petConfigName);

      if (petType == null) {
        logger.warning("Pet type not found for config name: " + petConfigName);
        return;
      }

      // Call petService.setPlayerPetType(player, petType)
      Class<?> serviceClass = petService.getClass();
      Class<?> petTypeClass = Class.forName("com.aincraft.jobpets.api.pet.PetType");
      serviceClass.getMethod("setPlayerPetType", Player.class, petTypeClass)
          .invoke(petService, player, petType);

      logger.info("Synced pet type '" + petConfigName + "' to JobPets-Core for " + player.getName());

    } catch (Exception e) {
      logger.warning("Failed to sync pet type to JobPets-Core: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
