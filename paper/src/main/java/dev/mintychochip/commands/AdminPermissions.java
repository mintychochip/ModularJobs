package dev.mintychochip.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

/** Central helper for the plugin's admin permission checks. */
public final class AdminPermissions {

  /** Root admin node declared in {@code plugin.yml}. */
  public static final String ADMIN = "modularjobs.admin";

  /** Utility holder; not instantiable. */
  private AdminPermissions() {}

  /**
   * Returns whether the command source's sender holds the admin permission.
   *
   * @param source command source whose sender is checked
   * @return whether the source's sender holds the admin permission
   */
  public static boolean isAdmin(CommandSourceStack source) {
    return isAdmin(source.getSender());
  }

  /**
   * Returns whether the sender holds the admin permission.
   *
   * @param sender sender to check, may be {@code null}
   * @return whether the sender holds the admin permission
   */
  public static boolean isAdmin(CommandSender sender) {
    return sender != null && sender.hasPermission(ADMIN);
  }
}
