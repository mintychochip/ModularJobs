package net.aincraft.container;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import net.aincraft.container.boost.Condition;

public interface RuledBoostSource extends BoostSource {

  Collection<Rule> rules();

  Policy policy();

  record Rule(Condition condition, int priority, Boost boost) {

  }

  interface Policy {

    List<Boost> resolve(Queue<Rule> rules);
  }


}
