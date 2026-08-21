package dev.mintychochip.payable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.mintychochip.JobProgressionView;
import dev.mintychochip.container.ExperiencePayableHandler.ExperienceBarContext;
import dev.mintychochip.container.ExperiencePayableHandler.ExperienceBarController;
import dev.mintychochip.container.ExperiencePayableHandler.ExperienceBarFormatter;
import dev.mintychochip.gui.craftux.CraftuxSurfaces;
import dev.mintychochip.util.PlayerJobCompositeKey;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import dev.craftux.api.theme.StyleRole;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * XP boss bars via craftux {@link CraftuxSurfaces#showBossBar}.
 *
 * <p>Still uses {@link ExperienceBarFormatter} to compute title/progress/color
 * (API contract), then mounts the result through craftux's boss-bar renderer.
 */
final class ExperienceBarControllerImpl implements ExperienceBarController, Listener {

  private final Cache<PlayerJobCompositeKey, BossBar> formatScratch = Caffeine.newBuilder()
      .build();

  private final Map<PlayerJobCompositeKey, BukkitTask> removalTasks = new HashMap<>();
  private final Map<PlayerJobCompositeKey, BigDecimal> bufferedAmounts = new HashMap<>();

  private final Plugin plugin;
  private final CraftuxSurfaces surfaces;

  ExperienceBarControllerImpl(Plugin plugin, CraftuxSurfaces surfaces) {
    this.plugin = plugin;
    this.surfaces = surfaces;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void display(ExperienceBarContext context, ExperienceBarFormatter formatter) {
    Player player = Bukkit.getPlayer(context.playerId());
    if (player == null) {
      return;
    }
    JobProgressionView progression = context.progression();
    PlayerJobCompositeKey compositeKey = PlayerJobCompositeKey.create(player, progression.job());

    BigDecimal merged = bufferedAmounts.merge(compositeKey, context.amount(), BigDecimal::add);
    ExperienceBarContext mergedContext =
        new ExperienceBarContext(progression, context.playerId(), merged);

    BossBar scratch = formatScratch.get(compositeKey,
        ignored -> BossBar.bossBar(Component.empty(), 0.0f, Color.BLUE, Overlay.PROGRESS));
    if (scratch == null) {
      return;
    }
    formatter.format(scratch, mergedContext);

    String titlePlain = PlainTextComponentSerializer.plainText().serialize(scratch.name());
    String mini = MiniMessage.miniMessage().serialize(scratch.name());
    String barKey = compositeKey.jobKey().asString();
    surfaces.showBossBar(
        player.getUniqueId(),
        barKey,
        titlePlain,
        mini,
        scratch.progress(),
        roleFor(scratch.color()));

    BukkitTask previous = removalTasks.get(compositeKey);
    if (previous != null && !previous.isCancelled()) {
      previous.cancel();
    }
    removalTasks.put(compositeKey, new BukkitRunnable() {
      @Override
      public void run() {
        surfaces.hideBossBar(player.getUniqueId(), barKey);
        bufferedAmounts.remove(compositeKey);
        formatScratch.invalidate(compositeKey);
        removalTasks.remove(compositeKey);
      }
    }.runTaskLater(plugin, 50L));
  }

  @EventHandler
  void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    surfaces.hideAllBossBars(playerId);
    formatScratch.asMap().keySet().removeIf(key -> {
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

  private static StyleRole roleFor(BossBar.Color color) {
    return switch (color) {
      case GREEN -> StyleRole.PROGRESS;
      case YELLOW -> StyleRole.WARNING;
      case RED -> StyleRole.DANGER;
      case BLUE, PURPLE -> StyleRole.ACCENT;
      case PINK -> StyleRole.HEADING;
      default -> StyleRole.BODY;
    };
  }
}
