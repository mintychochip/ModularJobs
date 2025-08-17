package net.aincraft.container;

import java.util.BitSet;

public final class SlotSetImpl implements SlotSet {

  private final BitSet bitset;

  public SlotSetImpl() {
    this.bitset = new BitSet(64);
  }

  public SlotSetImpl(long mask) {
    this.bitset = BitSet.valueOf(new long[]{mask});
  }

  @Override
  public boolean contains(int slot) {
    return bitset.get(slot);
  }

  @Override
  public long asLong() {
    long[] arr = bitset.toLongArray();
    return arr.length > 0 ? arr[0] : 0L;
  }
}
