package net.aincraft.api.container;

import java.util.Collection;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public class RuledBoostSourceImpl implements RuledBoostSource {

  private final List<Rule> rules;
  public RuledBoostSourceImpl(List<Rule> rules) {
    this.rules = rules;
  }

  @Override
  public Collection<Rule> rules() {
    return rules;
  }

  @Override
  public @NotNull List<Boost> evaluate(BoostContext context) {
    return List.of();
  }

  @Override
  public @NotNull Key key() {
    return null;
  }
}
