package net.aincraft;

import java.sql.SQLException;
import net.aincraft.api.Bridge;
import net.aincraft.api.container.PayableTypes;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.bridge.BridgeImpl;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.database.ConnectionSource;
import net.aincraft.database.ConnectionSourceFactory;
import net.aincraft.listener.JobListener;
import net.aincraft.payable.ExperiencePayableHandlerImpl;
import net.aincraft.service.ProgressionServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class Jobs extends JavaPlugin {

  @Nullable
  private ConnectionSource connectionSource = null;

  @Override
  public void onEnable() {
    Bukkit.getServicesManager()
        .register(Bridge.class, new BridgeImpl(), this, ServicePriority.High);
    YamlConfiguration config = YamlConfiguration.create(this, "config.yml");
    ConnectionSourceFactory factory = new ConnectionSourceFactory(this, config);
    try {
      connectionSource = factory.create();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    RegistryContainer.registryContainer()
        .editRegistry(RegistryKeys.PAYABLE_HANDLERS, registry -> {
          registry.register(new ExperiencePayableHandlerImpl(PayableTypes.EXPERIENCE.key(),
                  new ProgressionServiceImpl(connectionSource)));
        });
    Bukkit.getPluginManager().registerEvents(new JobListener(), this);
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
