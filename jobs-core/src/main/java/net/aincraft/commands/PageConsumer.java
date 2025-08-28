package net.aincraft.commands;

import org.bukkit.command.CommandSender;

public interface PageConsumer<T> {

  void consume(Page<T> page, CommandSender sender);

}
