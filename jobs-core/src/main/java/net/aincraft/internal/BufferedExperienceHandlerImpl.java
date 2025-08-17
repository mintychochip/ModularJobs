package net.aincraft.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.ExperiencePayableHandler;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.service.ProgressionService;
import net.aincraft.util.PlayerJobCompositeKey;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

final class BufferedExperienceHandlerImpl implements
    ExperiencePayableHandler {

  private ExperienceBarController controller;

  private ExperienceBarFormatter formatter;

  BufferedExperienceHandlerImpl(Plugin plugin) {
    this.formatter = new ExperienceBarFormatterImpl();
    this.controller = new ExperienceBarControllerImpl(plugin);
  }

  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    OfflinePlayer player = context.getPlayer();
    Job job = context.getJob();
    JobProgression progression = ProgressionService.progressionService().get(player,job);
    if (progression == null) {
      return;
    }
    Payable payable = context.getPayable();
    PayableAmount amount = payable.amount();
    BigDecimal amountDecimal = amount.amount();
    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    ProgressionService.progressionService().update(calculatedProgression);
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
