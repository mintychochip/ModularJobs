package net.aincraft.api;

import java.util.List;
import net.aincraft.api.container.Payable;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface Job extends Keyed {
  Component getDisplayName();
  Component getDescription();
  <C> JobTask getTask(@NotNull ActionType.Resolving<C> resolving, @NotNull C object) throws IllegalArgumentException;
  <C> void addTask(ActionType.Resolving<C> resolving, @NotNull C object, List<Payable> payables);
}
