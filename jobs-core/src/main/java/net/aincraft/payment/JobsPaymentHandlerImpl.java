package net.aincraft.payment;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.PayableCurve;
import net.aincraft.PayableCurve.Parameters;
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
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


final class JobsPaymentHandlerImpl implements JobsPaymentHandler {

  private final Plugin plugin;
  private final BoostEngine boostEngine;
  private final JobService jobService;

  @Inject
  public JobsPaymentHandlerImpl(Plugin plugin, BoostEngine boostEngine,
      JobService jobService) {
    this.plugin = plugin;
    this.boostEngine = boostEngine;
    this.jobService = jobService;
  }

  @Override
  public void pay(OfflinePlayer player, ActionType type, Context context) {
    List<JobProgression> progressions = jobService.getProgressions(player);
    for (JobProgression progression : progressions) {
      Job job = progression.job();
      JobTask task = jobService.getTask(job, type, context);
      task.payables().forEach(payable -> {
        PayableType payableType = payable.type();
        PayableAmount amount = payable.amount();
        Parameters parameters = new Parameters(amount.value(), progression.level(),
            progressions.size());
        PayableCurve curve = job.payableCurves().get(type.key());
        BigDecimal baseAmount = curve == null ? amount.value() : curve.evaluate(parameters);

        // Evaluate and apply boosts
        Payable basePayable = new Payable(payableType,
            PayableAmount.create(baseAmount, amount.currency().orElse(null)));
        Map<Key, Boost> boosts = boostEngine.evaluate(player, type, context, progression, basePayable);
        BigDecimal boostedAmount = baseAmount;
        for (Boost boost : boosts.values()) {
          boostedAmount = boost.boost(boostedAmount);
        }

        Payable finalPayable = new Payable(payableType,
            PayableAmount.create(boostedAmount, amount.currency().orElse(null)));
        PayableHandler handler = payableType.handler();
        handler.pay(new PayableContext(player, finalPayable, progression));
      });
    }
  }
}
