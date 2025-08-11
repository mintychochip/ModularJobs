package net.aincraft.api.container;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.container.ExperienceBarFormatter.FormattingContext;
import net.aincraft.api.service.ProgressionService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PayableTypes {

  private PayableTypes() {
    throw new UnsupportedOperationException();
  }

  public static final PayableType EXPERIENCE = type(new ExperiencePayableHandlerImpl(),
      "experience");

  public static final PayableType ECONOMY = type(context -> {
    Bridge.bridge().economy().deposit(context.getPlayer(), context.getPayable().getAmount());
  }, "economy");

  private static PayableType type(PayableHandler handler, String keyString) {
    return PayableType.create(handler, new NamespacedKey("jobs", keyString));
  }

  private static final class ExperiencePayableHandlerImpl implements PayableHandler {

    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> removal = new HashMap<>();

    @Override
    public void pay(PayableContext context) throws IllegalArgumentException {
      Player player = context.getPlayer();
      Job job = context.getJob();
      Payable payable = context.getPayable();
      ProgressionService progressionService = ProgressionService.progressionService();
      JobProgression progression = progressionService.get(player, job);
      if (progression == null) {
        progression = progressionService.create(player, job);
      }
      progression.addExperience(payable.getAmount().getAmount().doubleValue());
      progressionService.update(progression);
      BossBar bossBar = bossBars.computeIfAbsent(context.getPlayer().getUniqueId(),
          ignored -> BossBar.bossBar(Component.empty(), 0.0f, Color.BLUE, Overlay.PROGRESS));

      ExperienceBarFormatter.experienceBarFormatter()
          .format(bossBar, new FormattingContext(progression, payable, player));
      bossBar.addViewer(player);
      BukkitTask previous = removal.get(player.getUniqueId());
      if (previous != null && !previous.isCancelled()) {
        previous.cancel();
      }
      removal.put(player.getUniqueId(), new BukkitRunnable() {
        @Override
        public void run() {
          bossBar.removeViewer(player);
        }
      }.runTaskLater(Bridge.bridge().plugin(), 50L));
    }


  }

}
