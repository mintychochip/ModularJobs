package net.aincraft.player;

import net.kyori.adventure.text.Component;

public interface LineFormatter<T> {
  Component format(T data);
}
