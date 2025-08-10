package net.aincraft;

import java.sql.SQLException;
import java.util.List;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.JobTask;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableHandler;
import net.aincraft.api.container.PayableHandler.PayableContext;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.context.Context;
import net.aincraft.bridge.BridgeImpl;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.database.ConnectionSource;
import net.aincraft.database.ConnectionSourceFactory;
import net.aincraft.listener.JobListener;
import net.aincraft.service.ProgressionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class Jobs extends JavaPlugin {

  @Nullable
  private ConnectionSource connectionSource = null;

  @Override
  public void onEnable() {

    YamlConfiguration config = YamlConfiguration.create(this, "config.yml");
    ConnectionSourceFactory factory = new ConnectionSourceFactory(this, config);
    try {
      connectionSource = factory.create();
      Bukkit.getServicesManager()
          .register(Bridge.class, new BridgeImpl(this, connectionSource), this,
              ServicePriority.High);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
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

  public static void doTask(OfflinePlayer player, ActionType actionType,
      Context context) {
    ProgressionService progressionService = ProgressionService.progressionService();
    List<JobProgression> progressions = progressionService.getAll(player);
    for (JobProgression progression : progressions) {
      Job job = progression.getJob();
      @Nullable JobTask task = job.getTask(actionType, context);
      if (task == null) {
        continue;
      }
      for (Payable payable : task.getPayables()) {
        PayableType type = payable.getType();
        PayableHandler handler = type.handler();
        handler.pay(new PayableContext() {
          @Override
          public OfflinePlayer getPlayer() {
            return player;
          }

          @Override
          public Payable getPayable() {
            return payable;
          }

          @Override
          public Job getJob() {
            return job;
          }
        });
        Bukkit.broadcastMessage(
            "Paid player: " + player + " with type: " + type + " amount: " + payable.getAmount());
      }
    }
  }
}
