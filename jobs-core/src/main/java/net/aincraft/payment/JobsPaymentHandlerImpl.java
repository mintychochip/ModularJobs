package net.aincraft.payment;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.Job.PayableCurve.Parameters;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableHandler.PayableContext;
import net.aincraft.container.PayableType;
import net.aincraft.service.JobService;
import net.aincraft.service.ProgressionService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


final class JobsPaymentHandlerImpl implements JobsPaymentHandler {

  private final Plugin plugin;
  private final BoostEngine boostEngine;
  private final ProgressionService progressionService;
  private final JobService jobService;

  @Inject
  public JobsPaymentHandlerImpl(Plugin plugin, BoostEngine boostEngine, ProgressionService progressionService,
      JobService jobService) {
    this.plugin = plugin;
    this.boostEngine = boostEngine;
    this.progressionService = progressionService;
    this.jobService = jobService;
  }

  @Override
  public void pay(OfflinePlayer player, ActionType type, Context context) {
    List<JobProgression> progressions = progressionService.getAll(player);
    for (JobProgression progression : progressions) {
      List<Boost> boosts = boostEngine.evaluate(type, progression, (Player) player);
      Job job = progression.getJob();
      JobTask task = jobService.getTask(job, type, context);
      task.getPayables().forEach(payable -> {
        PayableType payableType = payable.type();
        PayableAmount amount = payable.amount();
        Parameters parameters = new Parameters(amount.value(), progression.getLevel(),
            progressions.size());

        BigDecimal finalAmount = job.getCurve(payableType)
            .map(c -> c.evaluate(parameters))
            .orElse(amount.value());
        Payable p = new Payable(payableType,
            PayableAmount.create(finalAmount,amount.currency().orElse(null)));
        PayableHandler handler = payableType.handler();
        handler.pay(new PayableContext(player, p, progression));
      });
    }
  }
}
