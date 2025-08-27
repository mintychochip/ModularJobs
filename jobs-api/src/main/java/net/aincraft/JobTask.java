package net.aincraft;

import java.util.List;
import net.aincraft.container.Payable;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a task within a job (e.g. breaking a block or killing a mob). Each task defines a set
 * of base {@link Payable} values that describe virtual rewards to be scaled and executed by the job
 * system.
 */
public record JobTask(Key jobKey, Key actionTypeKey, Key contextKey, List<Payable> payables) {

}
