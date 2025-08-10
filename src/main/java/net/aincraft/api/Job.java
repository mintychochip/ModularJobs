package net.aincraft.api;

import java.util.List;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.Payable;
import net.aincraft.api.context.Context;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Job extends Keyed {

  Component getDisplayName();
  Component getDescription();
  @Nullable
  JobTask getTask(@NotNull ActionType type, @NotNull Context object) throws IllegalArgumentException;
  void addTask(ActionType type, @NotNull Context object, List<Payable> payables);
  void addTask(ActionType type, Key key, List<Payable> payables);
}
