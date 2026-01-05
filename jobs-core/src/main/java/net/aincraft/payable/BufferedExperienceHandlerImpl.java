package net.aincraft.payable;

import com.google.inject.Inject;
import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.ExperiencePayableHandler;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.event.JobExperienceGainEvent;
import net.aincraft.event.JobLevelEvent;
import net.aincraft.service.JobService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class BufferedExperienceHandlerImpl implements
    ExperiencePayableHandler {

  private final ExperienceBarController controller;
  private final ExperienceBarFormatter formatter;
  private final JobService jobService;

  @Inject
  BufferedExperienceHandlerImpl(ExperienceBarController controller,
      ExperienceBarFormatter formatter, JobService jobService) {
    this.controller = controller;
    this.formatter = formatter;
    this.jobService = jobService;
  }


  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    OfflinePlayer player = context.player();
    JobProgression progression = context.jobProgression();
    Payable payable = context.payable();
    PayableAmount amount = payable.amount();
    BigDecimal amountDecimal = amount.value();

    // Fire experience gain event (pre-calculation)
    if (player.isOnline()) {
      Player onlinePlayer = player.getPlayer();
      Job job = progression.job();
      JobExperienceGainEvent expEvent = new JobExperienceGainEvent(onlinePlayer, job, progression, amountDecimal);
      Bukkit.getPluginManager().callEvent(expEvent);

      if (expEvent.isCancelled()) {
        return;
      }

      // Use potentially modified experience amount from event
      amountDecimal = expEvent.getExperienceGained();
    }

    int oldLevel = progression.level();
    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    int newLevel = calculatedProgression.level();

    if (jobService.update(calculatedProgression) && player.isOnline()) {
      Player onlinePlayer = player.getPlayer();

      // Fire level up event if level changed
      if (newLevel > oldLevel) {
        Job job = progression.job();
        JobLevelEvent levelEvent = new JobLevelEvent(onlinePlayer, job, oldLevel, newLevel, calculatedProgression);
        Bukkit.getPluginManager().callEvent(levelEvent);
      }

      controller.display(
          new ExperienceBarContext(calculatedProgression, onlinePlayer, amountDecimal),
          formatter);
    }
  }
}
