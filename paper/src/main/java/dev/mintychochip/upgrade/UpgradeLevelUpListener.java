package dev.mintychochip.upgrade;

import java.util.Optional;
import dev.mintychochip.Job;
import dev.mintychochip.paper.event.BukkitJobLeaveEvent;
import dev.mintychochip.paper.event.BukkitJobLevelEvent;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.util.Messages;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Awards skill points on job level-up and clears skill tree state on leave.
 * v2 trees take precedence for point awards; legacy jobs use {@link UpgradeTree}.
 */
public final class UpgradeLevelUpListener implements Listener {

  private final UpgradeService upgradeService;
  private final Registry<SkillTree> skillTreeRegistry;

  public UpgradeLevelUpListener(
      UpgradeService upgradeService, Registry<SkillTree> skillTreeRegistry) {
    this.upgradeService = upgradeService;
    this.skillTreeRegistry = skillTreeRegistry;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJobLevelUp(BukkitJobLevelEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int newLevel = event.getNewLevel();

    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    int awarded = awardPointsForLevel(playerId, jobKey, newLevel);
    if (awarded <= 0) {
      return;
    }

    int available;
    Optional<SkillTree> v2TreeOpt = skillTreeFor(jobKey);
    if (v2TreeOpt.isPresent()) {
      SkillTreeState updated = upgradeService.getSkillTreeState(playerId, jobKey);
      available = updated.totalSkillPoints() - v2TreeOpt.get().spentPoints(updated);
    } else {
      available = upgradeService.getPlayerData(playerId, jobKey).availableSkillPoints();
    }

    String pointsText = awarded > 1 ? "Skill Points" : "Skill Point";
    String message =
        String.format(
            "<accent>+<primary> %d %s<neutral> (<secondary>%d available<neutral>)",
            awarded, pointsText, available);
    Messages.send(player, message);
  }

  /**
   * Awards the delta between expected total points for {@code newLevel} and current total.
   *
   * @return points awarded, or 0 when no tree or already at expected total
   */
  int awardPointsForLevel(String playerId, String jobKey, int newLevel) {
    Optional<SkillTree> v2TreeOpt = skillTreeFor(jobKey);
    if (v2TreeOpt.isPresent()) {
      SkillTree tree = v2TreeOpt.get();
      int expected = newLevel * tree.skillPointsPerLevel();
      int current = upgradeService.getSkillTreeState(playerId, jobKey).totalSkillPoints();
      int pointsToAward = expected - current;
      if (pointsToAward <= 0) {
        return 0;
      }
      upgradeService.awardSkillPoints(playerId, jobKey, pointsToAward);
      return pointsToAward;
    }

    Optional<UpgradeTree> treeOpt = upgradeService.getTree(jobKey);
    if (treeOpt.isEmpty()) {
      return 0;
    }

    int expected = newLevel * treeOpt.get().skillPointsPerLevel();
    int current = upgradeService.getPlayerData(playerId, jobKey).totalSkillPoints();
    int pointsToAward = expected - current;
    if (pointsToAward <= 0) {
      return 0;
    }
    upgradeService.awardSkillPoints(playerId, jobKey, pointsToAward);
    return pointsToAward;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLeave(BukkitJobLeaveEvent event) {
    String playerId = event.getPlayer().getUniqueId().toString();
    String jobKey = event.getJob().key().value();
    upgradeService.clearTreeState(playerId, jobKey);
  }

  private Optional<SkillTree> skillTreeFor(String jobKey) {
    return skillTreeRegistry.get(Key.key("modularjobs", "upgrade_tree/" + jobKey));
  }
}
