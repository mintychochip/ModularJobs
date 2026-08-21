package net.aincraft;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.sql.SQLException;
import java.util.List;
import net.aincraft.commands.JobsCommand;
import net.aincraft.placeholders.PlaceholderExpansionHandle;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Paper plugin entry point that creates, registers, and tears down the
 * {@link PluginContext} composition root.
 */
@NullMarked
public final class ModularJobsBootstrap extends JavaPlugin {

  @Nullable
  private PluginContext context = null;

  /** Starts the plugin services, listeners, integrations, and command tree. */
  @Override
  public void onEnable() {
    PluginContext created = PluginContext.create(this);
    this.context = created;

    PluginProvider.set(this);
    Bridge.register(created.bridge);
    Bukkit.getServicesManager()
        .register(Bridge.class, created.bridge, this, ServicePriority.High);

    // ProfessionService is the stable integration point for dependent plugins.
    Bukkit.getServicesManager().register(
        net.aincraft.service.ProfessionService.class,
        created.bridge.professionService(), this, ServicePriority.Normal);

    // Auxiliary profession APIs may be stubs — only expose them when explicitly enabled.
    if (getConfig().getBoolean("profession-apis.register-bukkit-services", false)) {
      getSLF4JLogger().info(
          "Registering auxiliary profession Bukkit services "
              + "(profession-apis.register-bukkit-services=true). "
              + "Station/NodeHarvest may be stubs — see README.");
      Bukkit.getServicesManager().register(
          net.aincraft.service.RecipeService.class,
          created.bridge.recipeService(), this, ServicePriority.Normal);
      Bukkit.getServicesManager().register(
          net.aincraft.service.BuffService.class,
          created.bridge.buffService(), this, ServicePriority.Normal);
      Bukkit.getServicesManager().register(
          net.aincraft.service.StationService.class,
          created.bridge.stationService(), this, ServicePriority.Normal);
      Bukkit.getServicesManager().register(
          net.aincraft.service.NodeHarvestService.class,
          created.bridge.nodeHarvestService(), this, ServicePriority.Normal);
    }

    for (Listener listener : created.listeners) {
      Bukkit.getPluginManager().registerEvents(listener, this);
    }

    if (created.placeholderExpansion != null) {
      created.placeholderExpansion.register();
    }

    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, c -> {
      for (String alias : List.of("jobs", "j")) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(alias);
        for (JobsCommand command : created.commands) {
          root.then(command.build());
        }
        c.registrar().register(root.build());
      }
    });
  }

  /** Unregisters integrations and closes resources created during startup. */
  @Override
  public void onDisable() {
    PluginContext ctx = this.context;
    this.context = null;
    if (ctx == null) {
      return;
    }
    try {
      PlaceholderExpansionHandle expansion = ctx.placeholderExpansion;
      if (expansion != null) {
        try {
          expansion.unregister();
        } catch (IllegalStateException | IllegalArgumentException e) {
          getSLF4JLogger().warn("Failed to unregister PlaceholderAPI expansion", e);
        }
      }
      // Unregister Bridge holder + Bukkit services registered by this plugin
      Bridge.unregister();
      PluginProvider.set(null);
      Bukkit.getServicesManager().unregisterAll(this);
      ctx.shutdown();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
