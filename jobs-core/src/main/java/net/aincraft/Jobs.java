package net.aincraft;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.sql.SQLException;
import java.util.Set;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.aincraft.commands.JobsCommand;
import net.aincraft.repository.ConnectionSource;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class Jobs extends JavaPlugin {

  @Nullable
  private ConnectionSource connectionSource = null;

  @Override
  public void onEnable() {
    Injector injector = Guice.createInjector(new PluginModule(this));
    Bridge bridge = injector.getInstance(Bridge.class);
    Bukkit.getServicesManager()
        .register(Bridge.class, bridge, this,
            ServicePriority.High);
    Set<Listener> listeners = injector.getInstance(
        Key.get(new TypeLiteral<>() {
        })
    );
    listeners.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      injector.getInstance(PlaceholderExpansion.class).register();
    }
    Set<JobsCommand> commands = injector.getInstance(Key.get(new TypeLiteral<>(){}));
    LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("jobs");
    for (JobsCommand command : commands) {
      root.then(command.build());
    }
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,c -> {
      c.registrar().register(root.build());
    });
    Bukkit.getPluginCommand("test").setExecutor(injector.getInstance(Command.class));
  }


  @Override
  public void onDisable() {
    if (!(connectionSource == null || connectionSource.isClosed())) {
      try {
        connectionSource.shutdown();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
