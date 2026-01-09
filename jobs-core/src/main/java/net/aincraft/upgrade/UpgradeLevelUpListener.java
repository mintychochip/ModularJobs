package net.aincraft.upgrade;

import com.google.inject.Inject;
import dev.mintychochip.mint.Mint;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.event.JobLevelEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener that awards skill points when a player levels up in a job.
 */
public final class UpgradeLevelUpListener implements Listener {

  private final UpgradeService upgradeService;

  @Inject
  public UpgradeLevelUpListener(UpgradeService upgradeService) {
    this.upgradeService = upgradeService;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJobLevelUp(JobLevelEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
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

    // Get current player data to check existing skill points
    PlayerUpgradeData data = upgradeService.getPlayerData(playerId, jobKey);

    // Calculate expected total skill points for the new level
    int expectedTotalSkillPoints = newLevel * skillPointsPerLevel;
    int currentTotalSkillPoints = data.totalSkillPoints();

    // Only award if player has fewer skill points than expected
    int pointsToAward = expectedTotalSkillPoints - currentTotalSkillPoints;

    if (pointsToAward <= 0) {
      return; // Already has appropriate skill points
    }

    // Award the skill points
    upgradeService.awardSkillPoints(playerId, jobKey, pointsToAward);

    // Refresh data after awarding
    PlayerUpgradeData updatedData = upgradeService.getPlayerData(playerId, jobKey);

    // Use Mint's per-player theming with MiniMessage tags
    String pointsText = pointsToAward > 1 ? "Skill Points" : "Skill Point";
    String message = String.format("<accent>+<primary> %d %s<neutral> (<secondary>%d available<neutral>)",
        pointsToAward, pointsText, updatedData.availableSkillPoints());
    Mint.sendThemedMessage(player, message);
  }
}
