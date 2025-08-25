package net.aincraft.action;

import net.aincraft.container.ActionType;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record ActionTypeImpl(String name, Key key) implements ActionType {

  @Override
  public String toString() {
    return name;
  }
}
