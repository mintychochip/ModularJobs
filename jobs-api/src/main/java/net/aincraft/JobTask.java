package net.aincraft;

import java.util.List;
import net.aincraft.container.Payable;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a task within a job (e.g. breaking a block or killing a mob).
 * Each task defines a set of base {@link Payable} values that describe
 * virtual rewards to be scaled and executed by the job system.
 */
public interface JobTask {

  Key getContext();
  /**
   * Returns the unscaled, virtual rewards for this task.
   *
   * @return list of base {@link Payable} definitions
   */
  @NotNull
  List<Payable> getPayables();
}
