package net.aincraft.action;

import net.aincraft.container.ActionType;
import net.kyori.adventure.key.Key;

/** Immutable action type descriptor exposed through the public action contract. */
public record ActionTypeImpl(String name, Key key) implements ActionType {

  @Override
  public String toString() {
    return name;
  }
}
