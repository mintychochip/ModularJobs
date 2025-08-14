package net.aincraft.bridge;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.container.ExperiencePayableHandler;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableAmount;
import net.aincraft.api.container.PayableHandler;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.util.PlayerJobCompositeKey;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

final class BufferedExperienceHandlerImpl implements ExperiencePayableHandler {

  private final Map<PlayerJobCompositeKey, JobProgression> progressions;

  private ExperienceBarController controller;

  private ExperienceBarFormatter formatter;


  BufferedExperienceHandlerImpl(ExperienceBarController controller,
      ExperienceBarFormatter formatter,
      Map<PlayerJobCompositeKey, JobProgression> progressions) {
    this.controller = controller;
    this.formatter = formatter;
    this.progressions = progressions;
  }

  static PayableHandler create(Plugin plugin) {
    Map<PlayerJobCompositeKey, JobProgression> progressions = new ConcurrentHashMap<>();
    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
      if (progressions.isEmpty()) {
        return;
      }
      ProgressionService.progressionService().update(progressions.values().stream().toList());
      progressions.clear();
    }, 5L, 200L);
    ExperienceBarFormatterImpl formatter = new ExperienceBarFormatterImpl();
    ExperienceBarControllerImpl renderer = new ExperienceBarControllerImpl(plugin);
    return new BufferedExperienceHandlerImpl(renderer, formatter, progressions);
  }

  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    OfflinePlayer player = context.getPlayer();
    Job job = context.getJob();
    PlayerJobCompositeKey key = PlayerJobCompositeKey.create(player, job);
    JobProgression progression = this.progressions.computeIfAbsent(key,
        ignored -> ProgressionService.progressionService().get(player, job));
    if (progression == null) {
      return;
    }
    Payable payable = context.getPayable();
    PayableAmount amount = payable.getAmount();
    BigDecimal amountDecimal = amount.getAmount();
    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    progressions.put(key, calculatedProgression);
    if (player.isOnline()) {
      controller.display(
          new ExperienceBarContext(calculatedProgression, player.getPlayer(), amountDecimal),
          formatter);
    }
  }

  @Override
  public void setExperienceBarController(ExperienceBarController controller) {
    this.controller = controller;
  }

  @Override
  public void setExperienceBarFormatter(ExperienceBarFormatter formatter) {
    this.formatter = formatter;
  }

}
