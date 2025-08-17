package net.aincraft.container;

import java.util.Set;

public interface SlotSet {
  boolean contains(int slot);
  long asLong();

  interface Builder {

  }
}
