package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.gui.JobBrowseGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the job browse GUI.
 * Usage: /jobs browse
 */
public final class BrowseCommand implements JobsCommand {

  private final JobBrowseGui jobBrowseGui;

  @Inject
  public BrowseCommand(JobBrowseGui jobBrowseGui) {
    this.jobBrowseGui = jobBrowseGui;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("browse")
        .requires(source -> source.getSender().hasPermission("jobs.command.browse"))
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();

          if (!(sender instanceof Player player)) {
            Mint.sendThemedMessage(sender, "<error>This command can only be used by players.");
            return 0;
          }

          jobBrowseGui.open(player);
          return Command.SINGLE_SUCCESS;
        });
  }
}
