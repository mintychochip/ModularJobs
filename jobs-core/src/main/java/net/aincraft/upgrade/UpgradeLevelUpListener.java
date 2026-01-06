package net.aincraft.upgrade;

import com.google.inject.Inject;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.config.ColorScheme;
import net.aincraft.event.JobLevelEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener that awards skill points when a player levels up in a job.
 */
public final class UpgradeLevelUpListener implements Listener {

  private final UpgradeService upgradeService;
  private final ColorScheme colors;

  @Inject
  public UpgradeLevelUpListener(UpgradeService upgradeService, ColorScheme colors) {
    this.upgradeService = upgradeService;
    this.colors = colors;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJobLevelUp(JobLevelEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();

    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    // Check if this job has an upgrade tree
    Optional<UpgradeTree> treeOpt = upgradeService.getTree(jobKey);
    if (treeOpt.isEmpty()) {
      return; // No upgrade tree for this job
    }

    UpgradeTree tree = treeOpt.get();
    int skillPointsPerLevel = tree.skillPointsPerLevel();

    // Calculate total skill points to award (for level jumps)
    int levelsGained = newLevel - oldLevel;
    int totalPointsToAward = levelsGained * skillPointsPerLevel;

    if (totalPointsToAward <= 0) {
      return;
    }

    // Award the skill points
    upgradeService.awardSkillPoints(playerId, jobKey, totalPointsToAward);

    // Notify the player
    PlayerUpgradeData data = upgradeService.getPlayerData(playerId, jobKey);

    Component message = Component.text()
        .append(Component.text("+", colors.accent()))
        .append(Component.text(totalPointsToAward + " Skill Point" + (totalPointsToAward > 1 ? "s" : ""), colors.primary()))
        .append(Component.text(" (", colors.neutral()))
        .append(Component.text(data.availableSkillPoints() + " available", colors.secondary()))
        .append(Component.text(")", colors.neutral()))
        .build();

    player.sendMessage(message);
  }
}
