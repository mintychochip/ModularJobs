package net.aincraft.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Provides the Brigadier command tree for one subcommand of the jobs command.
 */
@FunctionalInterface
public interface JobsCommand {

  /**
   * Builds this command's literal and argument tree.
   *
   * @return command tree registered beneath the jobs root
   */
  LiteralArgumentBuilder<CommandSourceStack> build();
}
