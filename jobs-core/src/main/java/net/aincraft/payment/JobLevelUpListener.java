package net.aincraft.payment;

import com.google.inject.Inject;
import net.aincraft.event.JobLevelEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

final class JobLevelUpListener implements Listener {

  @Inject
  JobLevelUpListener() {
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLevelUp(JobLevelEvent event) {
    Player player = event.getPlayer();
    int newLevel = event.getNewLevel();
    int oldLevel = event.getOldLevel();

    // Play level up sound
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

    // Send level up message
    Component message = Component.text()
        .append(Component.text("You are level ", NamedTextColor.GRAY))
        .append(Component.text(newLevel, NamedTextColor.YELLOW))
        .append(Component.text(" ", NamedTextColor.GRAY))
        .append(event.getJob().displayName())
        .build();

    player.sendMessage(message);
  }
}
