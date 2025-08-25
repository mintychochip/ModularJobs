package net.aincraft.job;

import com.google.inject.Inject;
import net.aincraft.container.ActionType;
import net.aincraft.job.model.ActionTypeRecord;
import net.aincraft.registry.Registry;
import net.aincraft.util.KeyFactory;
import net.aincraft.util.Mapper;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class ActionTypeRecordMapper implements Mapper<ActionType, ActionTypeRecord> {

  private final Registry<ActionType> actionTypeRegistry;
  private final KeyFactory keyFactory;

  @Inject
  public ActionTypeRecordMapper(Registry<ActionType> actionTypeRegistry, KeyFactory keyFactory) {
    this.actionTypeRegistry = actionTypeRegistry;
    this.keyFactory = keyFactory;
  }

  @Override
  public @NotNull ActionType toDomainObject(@NotNull ActionTypeRecord record)
      throws IllegalArgumentException {
    Key key = keyFactory.create(record.actionTypeKey());
    return actionTypeRegistry.getOrThrow(key);
  }
}
