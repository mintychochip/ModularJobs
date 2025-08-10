package net.aincraft.listener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.api.ActionTypes;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.PayableTypes;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.container.JobImpl;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class JobListener implements Listener {

  static {
    RegistryContainer.registryContainer().editRegistry(RegistryKeys.JOBS, registry -> {
      JobImpl job = new JobImpl(Key.key("jobs:builder"),Component.text("fisherman"), Component.text("test"));
      List<Payable> payables = new ArrayList<>();
      payables.add(new Payable() {
        @Override
        public PayableType getType() {
          return PayableTypes.EXPERIENCE;
        }

        @Override
        public BigDecimal getAmount() {
          return BigDecimal.TWO;
        }
      });
      job.addTask(ActionTypes.BLOCK_PLACE,Material.STONE,payables);
      registry.register(job);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockPlace(final BlockPlaceEvent event) {
    if (!event.canBuild()) {
      return;
    }
    Player player = event.getPlayer();
    if (player.isInsideVehicle()) {
      return;
    }
    RegistryView<Job> jobRegistry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.JOBS);
    try {
      Job job = jobRegistry.getOrThrow(Key.key("jobs:builder"));
      JobTask task = job.getTask(ActionTypes.BLOCK_PLACE, event.getBlock().getType());
      List<Payable> base = task.getPayables();
      for (Payable payable : base) {
        PayableType type = payable.getType();
        Bukkit.broadcastMessage(payable.getAmount().toString());
      }
    } catch (IllegalArgumentException ignored) {
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockBreak(final BlockBreakEvent event) {

  }

}
