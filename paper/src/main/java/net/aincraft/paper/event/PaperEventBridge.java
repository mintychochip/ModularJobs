package net.aincraft.paper.event;

import java.util.Objects;
import net.aincraft.event.EventBus;
import net.aincraft.event.JobExperienceGainEvent;
import net.aincraft.event.JobJoinEvent;
import net.aincraft.event.JobLeaveEvent;
import net.aincraft.event.JobLevelEvent;
import net.aincraft.event.JobsPaymentEvent;
import net.aincraft.event.JobsPrePaymentEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Dual-fires pure domain events on {@link EventBus} and thin Bukkit wrappers for third parties.
 * Cancellable wrappers share cancel/mutate state with the pure event.
 */
public final class PaperEventBridge {

  private final EventBus bus;

  public PaperEventBridge(EventBus bus) {
    this.bus = Objects.requireNonNull(bus, "bus");
  }

  public JobLevelEvent publishLevel(JobLevelEvent pure, @Nullable Player playerForBukkit) {
    bus.publish(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobLevelEvent(playerForBukkit, pure));
    }
    return pure;
  }

  public JobJoinEvent publishJoin(JobJoinEvent pure, @Nullable Player playerForBukkit) {
    bus.publish(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobJoinEvent(playerForBukkit, pure));
    }
    return pure;
  }

  public JobLeaveEvent publishLeave(JobLeaveEvent pure, @Nullable Player playerForBukkit) {
    bus.publish(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobLeaveEvent(playerForBukkit, pure));
    }
    return pure;
  }

  public JobExperienceGainEvent publishExperienceGain(
      JobExperienceGainEvent pure, @Nullable Player playerForBukkit) {
    bus.publish(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobExperienceGainEvent(playerForBukkit, pure));
    }
    return pure;
  }

  public JobsPaymentEvent publishPayment(
      JobsPaymentEvent pure, @Nullable OfflinePlayer playerForBukkit) {
    bus.publish(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobsPaymentEvent(playerForBukkit, pure));
    }
    return pure;
  }

  public JobsPrePaymentEvent publishPrePayment(
      JobsPrePaymentEvent pure, @Nullable OfflinePlayer playerForBukkit) {
    bus.publish(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobsPrePaymentEvent(playerForBukkit, pure));
    }
    return pure;
  }
}
