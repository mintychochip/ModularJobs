package net.aincraft.api.container.boost;

import java.util.HashMap;
import java.util.Map;

public class ConditionCodecImpl implements ConditionCodec, ConditionAdapter.Writer, ConditionAdapter.Reader {

  private final Map<Byte, ConditionAdapter<?>> idToAdapter = new HashMap<>();
  private final Map<Class<?>, ConditionAdapter<?>> typeToAdapter = new HashMap<>();

  public <T extends Condition> void register(ConditionAdapter<T> adapter) {
    idToAdapter.put(adapter.id(), adapter);
    typeToAdapter.put(adapter.type(), adapter);
  }

  @Override
  public byte[] encode(Condition condition) {
    Out out = new Out(64);
    write(out, condition);
    return out.toByteArray();
  }

  @Override
  public Condition decode(byte[] bytes) {
    In in = new In(bytes);
    return read(in);
  }

  @Override
  public void write(Out out, Condition condition) {
    @SuppressWarnings("unchecked")
    ConditionAdapter<Condition> adapter = (ConditionAdapter<Condition>) typeToAdapter.get(condition.getClass());

    if (adapter == null) {
      throw new IllegalArgumentException("No adapter for type: " + condition.getClass());
    }

    out.writeByte(adapter.id());
    adapter.write(out, condition, this);
  }

  @Override
  public Condition read(In in) {
    byte id = (byte) in.readByte();
    ConditionAdapter<?> adapter = idToAdapter.get(id);

    if (adapter == null) {
      throw new IllegalArgumentException("Unknown adapter ID: " + id);
    }

    return adapter.read(in, this);
  }
}
