package net.aincraft.container;

public interface SlotSet {
  boolean contains(int slot);
  long asLong();

  interface Builder {

  }
}
