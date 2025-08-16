package net.aincraft.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.time.Duration;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

final class ExperienceBarControllerImpl implements ExperienceBarController {

  private final Cache<UUID, BossBar> bossBarCache = Caffeine.newBuilder().expireAfterWrite(
      Duration.ofMinutes(10)).build();

  private final Map<UUID, BukkitTask> removalTasks = new HashMap<>();

  private final Map<PlayerJobCompositeKey,BigDecimal> bufferedAmounts = new HashMap<>();

  private final Plugin plugin;

  ExperienceBarControllerImpl(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void display(ExperienceBarContext context, ExperienceBarFormatter formatter) {
    Player player = context.player();
    UUID uniqueId = player.getUniqueId();
    BossBar bossBar = bossBarCache.get(uniqueId,
        ignored -> BossBar.bossBar(Component.empty(), 0.0f, Color.BLUE, Overlay.PROGRESS));
    if (bossBar == null) {
      return;
    }
    JobProgressionView progression = context.progression();
    PlayerJobCompositeKey compositeKey = PlayerJobCompositeKey.create(player, progression.getJob());
    BigDecimal merged = bufferedAmounts.merge(compositeKey, context.amount(), BigDecimal::add);
    formatter.format(bossBar, new ExperienceBarContext(progression,player,merged));
    bossBar.addViewer(player);
    BukkitTask previous = removalTasks.get(uniqueId);
    if (previous != null && !previous.isCancelled()) {
      previous.cancel();
    }
    removalTasks.put(uniqueId, new BukkitRunnable() {
      @Override
      public void run() {
        bossBar.removeViewer(player);
        bufferedAmounts.remove(compositeKey);
      }
    }.runTaskLater(plugin, 50L));
  }
}
