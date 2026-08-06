package net.aincraft.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import net.aincraft.profession.TierAntiFarmEngine;
import net.aincraft.service.JobService;
import net.aincraft.util.KeyResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class JobsPaymentHandler {

  private final Plugin plugin;
  private final BoostEngine boostEngine;
  private final JobService jobService;
  @Nullable
  private final TierAntiFarmEngine antiFarmEngine;
  @Nullable
  private final KeyResolver keyResolver;

  public JobsPaymentHandler(Plugin plugin, BoostEngine boostEngine,
      JobService jobService) {
    this(plugin, boostEngine, jobService, null, null);
  }

  public JobsPaymentHandler(
      Plugin plugin,
      BoostEngine boostEngine,
      JobService jobService,
      @Nullable TierAntiFarmEngine antiFarmEngine,
      @Nullable KeyResolver keyResolver) {
    this.plugin = plugin;
    this.boostEngine = boostEngine;
    this.jobService = jobService;
    this.antiFarmEngine = antiFarmEngine;
    this.keyResolver = keyResolver;
  }

  public void pay(OfflinePlayer player, ActionType type, Context context) {
    List<JobProgression> progressions = jobService.getProgressions(player);
    for (JobProgression progression : progressions) {
      Job job = progression.job();
      JobTask task = jobService.getTask(job, type, context);
      if (task == null || task.payables() == null || task.payables().isEmpty()) {
        continue;
      }
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
        BigDecimal boostedAmount = BoostEngine.applyBoosts(baseAmount, boosts);

        // Tier anti-farm applies to experience only (AzothMC §8.2)
        if (antiFarmEngine != null && isExperience(payableType) && player.getUniqueId() != null) {
          String actionKey = buildActionKey(job, type, context);
          int tier = 1; // content tables supply real tiers later
          double mult = antiFarmEngine.evaluateAndRecord(
              player.getUniqueId(), actionKey, tier, progression.level());
          boostedAmount = applyMultiplier(boostedAmount, mult);
        }

        Payable finalPayable = new Payable(payableType,
            PayableAmount.create(boostedAmount, amount.currency().orElse(null)));
        PayableHandler handler = payableType.handler();
        handler.pay(new PayableContext(player, finalPayable, progression));
      });
    }
  }

  static BigDecimal applyMultiplier(BigDecimal amount, double multiplier) {
    if (multiplier <= 0) {
      return BigDecimal.ZERO;
    }
    if (multiplier >= 1.0d) {
      return amount;
    }
    return amount.multiply(BigDecimal.valueOf(multiplier)).setScale(4, RoundingMode.HALF_UP);
  }

  private static boolean isExperience(PayableType type) {
    Key key = type.key();
    return "experience".equals(key.value());
  }

  private String buildActionKey(Job job, ActionType type, Context context) {
    String contextPart = "unknown";
    if (keyResolver != null) {
      Key resolved = keyResolver.resolve(context);
      if (resolved != null) {
        contextPart = resolved.asString();
      }
    }
    return job.key().value() + "|" + type.key().value() + "|" + contextPart;
  }
}
