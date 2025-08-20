package net.aincraft;

import com.google.inject.Inject;
import me.clip.placeholderapi.PlaceholderAPI;
import net.aincraft.service.JobService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  private final JobService jobService;

  @Inject
  public Command(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {
    String set = PlaceholderAPI.setPlaceholders((Player) sender, "%modular_experience_builder%");
    Bukkit.broadcastMessage(set.toString());
    return false;
  }
}
