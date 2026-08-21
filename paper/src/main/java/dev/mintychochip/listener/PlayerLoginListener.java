package dev.mintychochip.listener;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.hooks.PermissionHook;
import dev.mintychochip.service.JobService;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Restores job perk permissions on login. */
public final class PlayerLoginListener implements Listener {

  private final JobService jobService;
  private final PermissionHook permissions;

  /** Player login listener. */
  public PlayerLoginListener(JobService jobService, PermissionHook permissions) {
    this.jobService = jobService;
    this.permissions = permissions;
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    List<JobProgression> progressions = jobService.getProgressions(player.getUniqueId());

    for (JobProgression progression : progressions) {
      Job job = progression.job();
      int level = progression.level();

      Map<Integer, List<String>> unlocks = job.perkUnlocks();
      String highestStoragePerk = null;
      for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
        if (entry.getKey() <= level) {
          for (String perk : entry.getValue()) {
            if (perk.startsWith("storage.")) {
              highestStoragePerk = perk;
            } else {
              permissions.grantPerkPermission(player, perk);
            }
          }
        }
      }
      if (highestStoragePerk != null) {
        permissions.grantPerkPermission(player, highestStoragePerk);
      }
    }
  }
}
