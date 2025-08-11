package net.aincraft.container;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.context.Context;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JobImpl implements Job {

  private final String key;
  private final Component displayName;
  private final Component description;

  private final Map<PayableType, PayableCurve> payableCurves;
  private final Map<ActionType, Registry<JobTask>> tasks = new HashMap<>();

  public JobImpl(String key, Component displayName, Component description, Map<PayableType,PayableCurve> payableCurves) {
    this.key = key;
    this.displayName = displayName;
    this.description = description;
    this.payableCurves = payableCurves;
  }

  @Override
  public Component getDisplayName() {
    return displayName;
  }

  @Override
  public Component getDescription() {
    return description;
  }

  @Override
  public PayableCurve getCurve(PayableType type) {
    return payableCurves.get(type);
  }

  @Override
  public void setCurve(PayableType type, PayableCurve curve) {
    payableCurves.put(type,curve);
  }

  @Override
  public int getMaxLevel() {
    return 200;
  }

  @Override
  public @Nullable JobTask getTask(@NotNull ActionType type, @NotNull Context object)
      throws IllegalArgumentException {
    KeyResolver keyResolver = KeyResolver.keyResolver();
    Registry<JobTask> registry = tasks.get(type);
    return registry.getOrThrow(keyResolver.resolve(object));
  }

  @Override
  public void addTask(ActionType type, @NotNull Context object, List<Payable> payables) {
    addTask(type, KeyResolver.keyResolver().resolve(object), payables);
  }

  @Override
  public void addTask(ActionType type, Key key, List<Payable> payables) {
    tasks.computeIfAbsent(type, ignored -> Registry.simple()).register(new JobTask() {
      @Override
      public @NotNull List<Payable> getPayables() {
        return payables;
      }

      @Override
      public @NotNull Key key() {
        return key;
      }
    });
  }

  @Override
  public @NotNull Key key() {
    return new NamespacedKey("jobs", key);
  }
}
