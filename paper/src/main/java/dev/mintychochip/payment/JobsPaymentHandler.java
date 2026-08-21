package dev.mintychochip.payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.PayableCurve.Parameters;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.Context;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableHandler.PayableContext;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.service.JobService;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Pays the player for a single {@link ActionType} across every job that defines a matching
 * task and has non-empty payables.
 *
 * <p>
 * For each payable, the base amount is run through the job's {@link PayableCurve} (when present),
 * then recipe experience depreciation (when configured), then boosted via {@link BoostEngine},
 * and finally handed to the payable type's {@link PayableHandler}. Progression is reloaded per
 * payable so sequential XP awards accumulate instead of last-write-wins against a single snapshot.
 */
public final class JobsPaymentHandler {

  private static final Key CRAFT_ACTION = Key.key("modularjobs", "craft");
  private static final Key EXPERIENCE_PAYABLE = Key.key("modularjobs", "experience");

  private final BoostEngine boostEngine;
  private final JobService jobService;
  private final @Nullable RecipeExperienceDepreciationApplier recipeDepreciation;

  /**
   * Creates the handler with the boost engine and progression service used to compute and
   * persist each payable.
   */
  public JobsPaymentHandler(Plugin plugin, BoostEngine boostEngine, JobService jobService) {
    this(plugin, boostEngine, jobService, null);
  }

  /**
   * Creates the handler with optional recipe experience depreciation for registered craft recipes.
   */
  public JobsPaymentHandler(
      Plugin plugin,
      BoostEngine boostEngine,
      JobService jobService,
      @Nullable RecipeExperienceDepreciationApplier recipeDepreciation) {
    Objects.requireNonNull(plugin, "plugin");
    this.boostEngine = boostEngine;
    this.jobService = jobService;
    this.recipeDepreciation = recipeDepreciation;
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
        baseAmount = applyRecipeDepreciation(player, type, context, payableType, baseAmount);
        if (baseAmount.signum() == 0) {
          continue;
        }

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

  private BigDecimal applyRecipeDepreciation(
      OfflinePlayer player,
      ActionType type,
      Context context,
      PayableType payableType,
      BigDecimal baseAmount) {
    if (recipeDepreciation == null
        || !CRAFT_ACTION.equals(type.key())
        || !EXPERIENCE_PAYABLE.equals(payableType.key())) {
      return baseAmount;
    }
    return recipeDepreciation.scaleCraftExperience(player.getUniqueId(), context, baseAmount);
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
