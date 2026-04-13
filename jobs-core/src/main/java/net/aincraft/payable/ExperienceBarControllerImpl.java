package net.aincraft.payable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.aincraft.JobProgressionView;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarContext;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarController;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.aincraft.util.PlayerJobCompositeKey;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

final class ExperienceBarControllerImpl implements ExperienceBarController, Listener {

  // weakValues() allows GC of BossBars when no longer referenced
  private final Cache<PlayerJobCompositeKey, BossBar> bossBarCache = Caffeine.newBuilder()
      .weakValues()
      .build();

  private final Map<PlayerJobCompositeKey, BukkitTask> removalTasks = new HashMap<>();

  private final Map<PlayerJobCompositeKey, BigDecimal> bufferedAmounts = new HashMap<>();

  private final Plugin plugin;

  @Inject
  ExperienceBarControllerImpl(Plugin plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void display(ExperienceBarContext context, ExperienceBarFormatter formatter) {
    Player player = context.player();
    JobProgressionView progression = context.progression();
    PlayerJobCompositeKey compositeKey = PlayerJobCompositeKey.create(player, progression.job());
    BossBar bossBar = bossBarCache.get(compositeKey,
        ignored -> BossBar.bossBar(Component.empty(), 0.0f, Color.BLUE, Overlay.PROGRESS));
    if (bossBar == null) {
      return;
    }
    BigDecimal merged = bufferedAmounts.merge(compositeKey, context.amount(), BigDecimal::add);
    formatter.format(bossBar, new ExperienceBarContext(progression,player,merged));

    // Only add viewer if not already viewing to prevent duplicate boss bars
    boolean isViewing = false;
    for (var viewer : bossBar.viewers()) {
      if (viewer.equals(player)) {
        isViewing = true;
        break;
      }
    }
    if (!isViewing) {
      bossBar.addViewer(player);
    }

    BukkitTask previous = removalTasks.get(compositeKey);
    if (previous != null && !previous.isCancelled()) {
      previous.cancel();
    }
    removalTasks.put(compositeKey, new BukkitRunnable() {
      @Override
      public void run() {
        bossBar.removeViewer(player);
        bufferedAmounts.remove(compositeKey);
        bossBarCache.invalidate(compositeKey);
      }
    }.runTaskLater(plugin, 50L));
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    // Clean up all boss bars, tasks, and buffered amounts for this player
    bossBarCache.asMap().keySet().removeIf(key -> {
      if (key.playerId().equals(playerId)) {
        BukkitTask task = removalTasks.remove(key);
        if (task != null) {
          task.cancel();
        }
        bufferedAmounts.remove(key);
        return true;
      }
      return false;
    });
  }
}
