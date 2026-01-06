package net.aincraft;

import java.util.List;
import java.util.Optional;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents a task within a job (e.g. breaking a block or killing a mob). Each task defines a set
 * of base {@link Payable} values that describe virtual rewards to be scaled and executed by the job
 * system.
 */
public record JobTask(Key jobKey, Key actionTypeKey, Key contextKey, @NotNull List<Payable> payables) {

  /**
   * Returns all payables of the given type.
   */
  public @UnmodifiableView List<Payable> payablesByType(PayableType type) {
    return payables.stream()
        .filter(p -> p.type().equals(type))
        .toList();
  }

  /**
   * Returns the first payable of the given type, if present.
   */
  public Optional<Payable> firstPayable(PayableType type) {
    return payables.stream()
        .filter(p -> p.type().equals(type))
        .findFirst();
  }

  /**
   * Returns the unique key identifying this task within a job.
   */
  public TaskKey key() {
    return new TaskKey(actionTypeKey, contextKey);
  }

  /**
   * A composite key for tasks within a job.
   */
  public record TaskKey(Key actionTypeKey, Key contextKey) {
    public String asString() {
      return actionTypeKey + "|" + contextKey;
    }
  }
}
