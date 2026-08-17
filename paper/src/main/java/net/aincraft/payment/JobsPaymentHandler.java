package net.aincraft.payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.PayableCurve;
import net.aincraft.PayableCurve.Parameters;
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
import org.bukkit.plugin.Plugin;

/**
 * Pays the player for a single {@link ActionType} across every job that defines a matching
 * task and has non-empty payables.
 * <p>
 * For each payable, the base amount is run through the job's {@link PayableCurve} (when present),
 * then boosted via {@link BoostEngine}, and finally handed to the payable type's
 * {@link PayableHandler}. Progression is reloaded per payable so sequential XP awards accumulate
 * instead of last-write-wins against a single snapshot.
 */
public final class JobsPaymentHandler {

  private final Plugin plugin;
  private final BoostEngine boostEngine;
  private final JobService jobService;

  /**
   * Creates the handler with the boost engine and progression service used to compute and
   * persist each payable.
   */
  public JobsPaymentHandler(Plugin plugin, BoostEngine boostEngine, JobService jobService) {
    this.plugin = plugin;
    this.boostEngine = boostEngine;
    this.jobService = jobService;
  }

  /**
   * Pays {@code player} for {@code type} across every job that has a matching task with
   * non-empty payables. Applies the payable curve and all active boosts before dispatching to
   * the payable handler, reloading progression per payable for correct accumulation.
   */
  public void pay(OfflinePlayer player, ActionType type, Context context) {
    List<JobProgression> progressions = jobService.getProgressions(player.getUniqueId());
    for (JobProgression initialProgression : progressions) {
      Job job = initialProgression.job();
      JobTask task = jobService.getTask(job, type, context);
      if (task == null || task.payables() == null || task.payables().isEmpty()) {
        continue;
      }
      String playerId = player.getUniqueId().toString();
      String jobKey = job.key().toString();
      // Reload progression per payable so sequential XP awards accumulate instead of
      // last-write-wins against a single snapshot.
      for (Payable payable : task.payables()) {
        JobProgression progression = reloadProgression(playerId, jobKey, initialProgression);
        if (progression == null) {
          continue;
        }
        PayableType payableType = payable.type();
        PayableAmount amount = payable.amount();
        Parameters parameters = new Parameters(
            amount.value(), progression.level(), progressions.size());
        PayableCurve curve = job.payableCurves().get(type.key());
        BigDecimal baseAmount = curve == null ? amount.value() : curve.evaluate(parameters);

        Payable basePayable = new Payable(
            payableType, PayableAmount.create(baseAmount, amount.currency().orElse(null)));
        Map<Key, Boost> boosts =
            boostEngine.evaluate(player, type, context, progression, basePayable);
        BigDecimal boostedAmount = BoostEngine.applyBoosts(baseAmount, boosts);

        Payable finalPayable = new Payable(
            payableType, PayableAmount.create(boostedAmount, amount.currency().orElse(null)));
        PayableHandler handler = payableType.handler();
        handler.pay(new PayableContext(player.getUniqueId(), finalPayable, progression));
      }
    }
  }

  /**
   * Fresh progression from the service (write-back cache aware). Falls back to the list snapshot
   * if reload returns null mid-pay (e.g. concurrent leave).
   */
  JobProgression reloadProgression(
      String playerId, String jobKey, JobProgression fallback) {
    JobProgression reloaded = jobService.getProgression(playerId, jobKey);
    return reloaded != null ? reloaded : fallback;
  }
}
