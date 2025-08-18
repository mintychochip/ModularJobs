package net.aincraft.payment;

import com.google.inject.Inject;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableHandler.PayableContext;
import net.aincraft.container.PayableType;
import net.aincraft.service.JobTaskProvider;
import net.aincraft.service.ProgressionService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;


final class JobsPaymentHandlerImpl implements JobsPaymentHandler{

  private final BoostEngine boostEngine;
  private final ProgressionService progressionService;
  private final JobTaskProvider jobTaskProvider;

  @Inject
  public JobsPaymentHandlerImpl(BoostEngine boostEngine, ProgressionService progressionService,
      JobTaskProvider jobTaskProvider) {
    this.boostEngine = boostEngine;
    this.progressionService = progressionService;
    this.jobTaskProvider = jobTaskProvider;
  }

  @Override
  public void pay(OfflinePlayer player, ActionType type, Context context) {
    List<JobProgression> progressions = progressionService.getAll(player);
    for (JobProgression progression : progressions) {
      List<Boost> boosts = boostEngine.evaluate(type, progression, (Player) player);
      Job job = progression.getJob();
      jobTaskProvider.getTask(job, type, context).ifPresent(task -> {
        for (Payable payable : task.getPayables()) {
          PayableType payableType = payable.type();
          PayableHandler handler = payableType.handler();
          handler.pay(new PayableContext(player,payable,job));
        }
      });
    }
  }
}
