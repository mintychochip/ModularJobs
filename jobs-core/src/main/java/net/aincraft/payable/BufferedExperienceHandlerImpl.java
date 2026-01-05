package net.aincraft.payable;

import com.google.inject.Inject;
import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.ExperiencePayableHandler;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.domain.repository.JobProgressionRepository;
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

    // Capture old level before adding experience
    int oldLevel = progression.level();
    Job job = progression.job();

    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    int newLevel = calculatedProgression.level();

    // Fire JobLevelEvent if level increased
    if (newLevel > oldLevel && player.isOnline()) {
      Player onlinePlayer = player.getPlayer();
      JobLevelEvent event = new JobLevelEvent(onlinePlayer, job, oldLevel, newLevel);
      Bukkit.getPluginManager().callEvent(event);
    }

    if (jobService.update(calculatedProgression) && player.isOnline()) {
      controller.display(
          new ExperienceBarContext(calculatedProgression, player.getPlayer(), amountDecimal),
          formatter);
    }
  }
}
