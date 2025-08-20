package net.aincraft.payment;

import com.google.inject.Inject;
import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.ExperiencePayableHandler;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.service.ProgressionService;
import org.bukkit.OfflinePlayer;

final class BufferedExperienceHandlerImpl implements
    ExperiencePayableHandler {

  private final ExperienceBarController controller;
  private final ExperienceBarFormatter formatter;
  private final ProgressionService progressionService;

  @Inject
  BufferedExperienceHandlerImpl(ExperienceBarController controller,
      ExperienceBarFormatter formatter, ProgressionService progressionService) {
    this.controller = controller;
    this.formatter = formatter;
    this.progressionService = progressionService;
  }


  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    OfflinePlayer player = context.player();
    JobProgression progression = context.jobProgression();
    Payable payable = context.payable();
    PayableAmount amount = payable.amount();
    BigDecimal amountDecimal = amount.value();
    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    progressionService.update(calculatedProgression);
    if (player.isOnline()) {
      controller.display(
          new ExperienceBarContext(calculatedProgression, player.getPlayer(), amountDecimal),
          formatter);
    }
  }
}
