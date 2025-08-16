package net.aincraft.api.container;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.boost.Condition;

public interface RuledBoostSource extends BoostSource {

  record Rule(Condition condition, int priority, Boost boost) {

  }

  interface Policy {

    List<Boost> resolve(Collection<Rule> rules, BoostContext context);
  }

  Collection<Rule> rules();

}
