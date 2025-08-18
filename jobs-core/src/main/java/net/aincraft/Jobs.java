package net.aincraft;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostSource;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableHandler.PayableContext;
import net.aincraft.container.PayableType;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.event.JobsPaymentEvent;
import net.aincraft.event.JobsPrePaymentEvent;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import net.aincraft.service.JobTaskProvider;
import net.aincraft.service.ProgressionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    Set<Listener> listeners = injector.getInstance(
        Key.get(new TypeLiteral<>() {
        })
    );
    listeners.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
//    Bukkit.getServicesManager()
//        .register(Bridge.class, new BridgeImpl(this, connectionSource), this,
//            ServicePriority.High);
//    Bukkit.getPluginCommand("test").setExecutor(new Command());
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
