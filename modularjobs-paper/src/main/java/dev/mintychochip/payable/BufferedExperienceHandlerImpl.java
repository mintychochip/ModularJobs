package dev.mintychochip.payable;

import dev.mintychochip.Bridge;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.container.ExperiencePayableHandler;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.event.JobExperienceGainEvent;
import dev.mintychochip.event.JobLevelEvent;
import dev.mintychochip.paper.event.PaperEventBridge;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Experience payable handler that buffers the award through the shared job {@link JobService}.
 * Fires a cancelable {@link JobExperienceGainEvent} before the award (allowing the amount to be
 * modified), skips already-capped progressions, persists the accumulated experience, fires a {@link
 * JobLevelEvent} on level-up, and updates the on-screen experience bar only for online players
 * whose update was persisted.
 */
final class BufferedExperienceHandlerImpl implements ExperiencePayableHandler {

  private final ExperienceBarController controller;
  private final ExperienceBarFormatter formatter;
  private final JobService jobService;

  /**
   * Creates the buffered experience handler wired to the given bar controller, formatter, and job
   * service.
   */
  BufferedExperienceHandlerImpl(
      ExperienceBarController controller, ExperienceBarFormatter formatter, JobService jobService) {
    this.controller = controller;
    this.formatter = formatter;
    this.jobService = jobService;
  }

  @Override
  public void pay(PayableContext context) {
    UUID playerId = context.playerId();
    OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
    Player onlinePlayer = player.isOnline() ? player.getPlayer() : null;
    JobProgression progression = context.jobProgression();
    Payable payable = context.payable();
    PayableAmount amount = payable.amount();
    BigDecimal amountDecimal = amount.value();
    PaperEventBridge events = new PaperEventBridge(Bridge.bridge().eventBus());

    // Fire experience gain event (pre-calculation); pure + Bukkit dual-fire
    Job job = progression.job();
    JobExperienceGainEvent expEvent =
        events.publishExperienceGain(
            new JobExperienceGainEvent(playerId, job, progression, amountDecimal), onlinePlayer);

    if (expEvent.isCancelled()) {
      return;
    }

    // Use potentially modified experience amount from event
    amountDecimal = expEvent.getExperienceGained();

    int oldLevel = progression.level();
    int maxLevel = progression.job().maxLevel();

    // Don't add experience if already at max level
    if (oldLevel >= maxLevel) {
      return;
    }

    JobProgression calculatedProgression = progression.addExperience(amountDecimal);
    int newLevel = calculatedProgression.level();

    if (jobService.update(calculatedProgression)) {
      // Fire level up event if level changed
      if (newLevel > oldLevel) {
        events.publishLevel(
            new JobLevelEvent(playerId, job, oldLevel, newLevel, calculatedProgression),
            onlinePlayer);
      }

      if (onlinePlayer != null) {
        controller.display(
            new ExperienceBarContext(calculatedProgression, playerId, amountDecimal), formatter);
      }
    }
  }
}
