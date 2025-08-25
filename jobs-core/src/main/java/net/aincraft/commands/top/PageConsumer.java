package net.aincraft.commands.top;

import org.bukkit.command.CommandSender;

public interface PageConsumer<T> {

  void consume(Page<T> page, CommandSender sender);

  record Page<T>(T content, int pageNumber, int pageSize) {

  }
}
