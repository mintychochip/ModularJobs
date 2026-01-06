package net.aincraft.boost.conditions;

import java.util.Set;
import net.aincraft.Job;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

public record JobConditionImpl(Set<String> jobKeys) implements Condition {

  public JobConditionImpl(String jobKey) {
    this(Set.of(jobKey));
  }

  public JobConditionImpl(String... jobKeys) {
    this(Set.of(jobKeys));
  }

  @Override
  public boolean applies(BoostContext context) {
    Job job = context.progression().job();
    String currentJobKey = job.key().asString();
    return jobKeys.contains(currentJobKey);
  }

}
