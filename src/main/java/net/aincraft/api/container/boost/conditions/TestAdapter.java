package net.aincraft.api.container.boost.conditions;

import net.aincraft.api.container.boost.ConditionAdapter;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import org.bukkit.NamespacedKey;

public class TestAdapter implements ConditionAdapter<BiomeCondition> {

  @Override
  public byte id() {
    return 0x0001;
  }

  @Override
  public Class<BiomeCondition> type() {
    return BiomeCondition.class;
  }

  @Override
  public void write(Out out, BiomeCondition condition, Writer writer) {
    out.writeString(condition.biomeKey().toString());
  }

  @Override
  public BiomeCondition read(In in, Reader reader) {
    return new BiomeCondition(NamespacedKey.fromString(in.readString()));
  }
}
