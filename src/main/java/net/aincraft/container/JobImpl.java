package net.aincraft.container;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.api.ActionType;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.PaymentCurve;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class JobImpl implements Job {

  private final Key key;
  private final Component displayName;
  private final Component description;

  private final Map<PayableType,PaymentCurve> paymentCurves = new HashMap<>();
  private final Map<ActionType, Registry<JobTask>> tasks = new HashMap<>();

  public JobImpl(Key key, Component displayName, Component description) {
    this.key = key;
    this.displayName = displayName;
    this.description = description;
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
  public <C> JobTask getTask(@NotNull ActionType.Resolving<C> resolving, @NotNull C object)
      throws IllegalArgumentException {
    KeyResolver<C> keyResolver = resolving.resolver();
    Registry<JobTask> registry = tasks.get(resolving);
    return registry.getOrThrow(keyResolver.resolve(object));
  }

  @Override
  public <C> void addTask(ActionType.Resolving<C> resolving, @NotNull C object, List<Payable> payables) {
    tasks.computeIfAbsent(resolving,
        ignored -> Registry.simple()).register(new JobTask() {
      @Override
      public @NotNull List<Payable> getPayables() {
        return payables;
      }

      @Override
      public @NotNull Key key() {
        return resolving.resolver().resolve(object);
      }
    });
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}
