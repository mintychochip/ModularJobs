package net.aincraft.container.boost;

import java.util.List;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;

public interface RuledBoostSource extends BoostSource {

  List<Rule> rules();

  record Rule(Condition condition, int priority, Boost boost) {

  }

}
