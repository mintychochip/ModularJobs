package net.aincraft.bridge;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
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
import org.jspecify.annotations.NonNull;

final class BufferedExperienceHandlerImpl implements
    ExperiencePayableHandler {

  private final Cache<PlayerJobCompositeKey, JobProgression> cache;

  private ExperienceBarController controller;

  private ExperienceBarFormatter formatter;

  BufferedExperienceHandlerImpl(
      Cache<PlayerJobCompositeKey, JobProgression> cache, Plugin plugin) {
    this.cache = cache;
    this.formatter = new ExperienceBarFormatterImpl();
    this.controller = new ExperienceBarControllerImpl(plugin);
  }


  static PayableHandler create(Plugin plugin) {
    Cache<PlayerJobCompositeKey, JobProgression> cache = Caffeine.newBuilder()
        .expireAfterWrite(
            Duration.ofMinutes(1)).build();
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      ConcurrentMap<PlayerJobCompositeKey, @NonNull JobProgression> liveView = cache.asMap();
      List<JobProgression> progressions = new ArrayList<>(liveView.values());
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        ProgressionService.progressionService().update(progressions);
      });
    }, 0L, 200L);
    return new BufferedExperienceHandlerImpl(cache, plugin);
  }

  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    OfflinePlayer player = context.getPlayer();
    Job job = context.getJob();
    PlayerJobCompositeKey key = PlayerJobCompositeKey.create(player, job);
    JobProgression progression = cache.get(key,
        ignored -> ProgressionService.progressionService().get(player, job));
    if (progression == null) {
      return;
    }
    Payable payable = context.getPayable();
    PayableAmount amount = payable.getAmount();
    BigDecimal amountDecimal = amount.getAmount();
    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    cache.put(key, calculatedProgression);
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
