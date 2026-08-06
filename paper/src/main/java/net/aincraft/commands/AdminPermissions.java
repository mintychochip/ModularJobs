package net.aincraft.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

/**
 * Shared admin permission checks for sensitive ModularJobs commands.
 */
public final class AdminPermissions {

  /** Root admin node declared in {@code plugin.yml}. */
  public static final String ADMIN = "modularjobs.admin";

  private AdminPermissions() {}

  public static boolean isAdmin(CommandSourceStack source) {
    return isAdmin(source.getSender());
  }

  public static boolean isAdmin(CommandSender sender) {
    return sender != null && sender.hasPermission(ADMIN);
  }
}
