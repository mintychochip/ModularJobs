package net.aincraft.api.container.boost;

public interface ConditionAdapter<C extends Condition> {
  byte id();
  Class<C> type();
  void write(Out out, C condition, Writer writer);
  C read(In in, Reader reader);

  interface Writer {
    void write(Out out, Condition node);
  }

  interface Reader {
    Condition read(In in);
  }
}
