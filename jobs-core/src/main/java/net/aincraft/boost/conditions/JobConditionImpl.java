package net.aincraft.boost.conditions;

import java.util.Set;
import net.aincraft.Job;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Record condition that checks if the current job matches any of the given job keys.
 * Delegates to {@link Conditions#job(Set)} for implementation.
 */
@Internal
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
