package net.aincraft.container.boost;

import java.util.List;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;

public interface RuledBoostSource extends BoostSource {

  List<Rule> rules();

  Policy policy();

  record Rule(Condition condition, int priority, Boost boost) {

  }

  interface Policy {

    List<Boost> resolve(Queue<Rule> rules);
  }


}
