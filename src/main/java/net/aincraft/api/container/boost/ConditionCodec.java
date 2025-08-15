package net.aincraft.api.container.boost;

public interface ConditionCodec {

  byte[] encode(Condition condition);

  Condition decode(byte[] bytes);

  interface Builder {
    Builder adapter(ConditionAdapter adapter);
    ConditionCodec build();
  }
}
