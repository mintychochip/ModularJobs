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
import org.bukkit.plugin.Plugin;

final class BufferedExperienceHandlerImpl implements
    ExperiencePayableHandler {

  private ExperienceBarController controller;

  private ExperienceBarFormatter formatter;

  @Inject
  BufferedExperienceHandlerImpl(Plugin plugin) {
    this.formatter = new ExperienceBarFormatterImpl();
    this.controller = new ExperienceBarControllerImpl(plugin);
  }

  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    OfflinePlayer player = context.player();
    Job job = context.job();
    JobProgression progression = ProgressionService.progressionService().get(player,job);
    if (progression == null) {
      return;
    }
    Payable payable = context.payable();
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
