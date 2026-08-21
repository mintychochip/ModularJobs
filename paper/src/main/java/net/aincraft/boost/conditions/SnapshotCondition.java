package net.aincraft.boost.conditions;

import dev.conditions.ConditionContext;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

/**
 * Adapts a Paper-free {@link dev.conditions.Condition} onto the boost
 * {@link Condition} interface by reading {@link BoostContext#conditions()}.
 */
public record SnapshotCondition(dev.conditions.Condition delegate) implements Condition {

  public static Condition wrap(dev.conditions.Condition delegate) {
    return new SnapshotCondition(delegate);
  }

  /**
   * Unwraps a boost condition to the snapshot graph, wrapping lambdas as
   * snapshot predicates.
   */
  public static dev.conditions.Condition unwrap(Condition condition) {
    if (condition instanceof SnapshotCondition snapshot) {
      return snapshot.delegate();
    }
    return ctx -> condition.applies(new BoostContext(null, null, null, null, null, ctx));
  }

  @Override
  public boolean applies(BoostContext context) {
    ConditionContext snapshot = context.conditions();
    if (snapshot == null) {
      snapshot = ConditionContext.absent();
    }
    return delegate.test(snapshot);
  }
}
