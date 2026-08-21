package dev.mintychochip.payment;

import dev.mintychochip.paper.event.BukkitJobLevelEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.time.Duration;

/**
 * Listens for job level-up events and plays celebratory feedback (sound,
 * title, and chat message) to the leveled player.
 */
final class JobLevelUpListener implements Listener {

  /** Creates the level-up listener with no dependencies. */
  JobLevelUpListener() {
  }

  /**
   * On MONITOR priority, plays the level-up and firework sounds, shows a
   * title, and sends a chat message when a player levels up in a job.
   */
  @EventHandler(priority = EventPriority.MONITOR)
  void onJobLevelUp(BukkitJobLevelEvent event) {
    Player player = event.getPlayer();
    int newLevel = event.getNewLevel();

    // Play level up sound
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    
    // Play celebration firework sound
    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 1.2f);

    // Show title display for prominent level up notification
    Component title = Component.text()
        .append(Component.text("Level Up!", NamedTextColor.GOLD, TextDecoration.BOLD))
        .build();
    
    Component subtitle = Component.text()
        .append(event.getJob().displayName())
        .append(Component.text(" Level ", NamedTextColor.GRAY))
        .append(Component.text(newLevel, NamedTextColor.YELLOW))
        .build();
    
    Title.Times times = Title.Times.times(
        Duration.ofMillis(500),   // Fade in
        Duration.ofMillis(2000),  // Stay
        Duration.ofMillis(500)    // Fade out
    );
    
    player.showTitle(Title.title(title, subtitle, times));

    // Send level up message
    Component message = Component.text()
        .append(Component.text("[", NamedTextColor.GRAY))
        .append(Component.text("Jobs", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.GRAY))
        .append(Component.text("You are now level ", NamedTextColor.GREEN))
        .append(Component.text(newLevel, NamedTextColor.YELLOW))
        .append(Component.text(" ", NamedTextColor.GREEN))
        .append(event.getJob().displayName())
        .append(Component.text("!", NamedTextColor.GREEN))
        .build();

    player.sendMessage(message);
  }
}
