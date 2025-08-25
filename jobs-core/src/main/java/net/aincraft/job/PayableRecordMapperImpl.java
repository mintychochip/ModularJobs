package net.aincraft.job;

import com.google.inject.Inject;
import java.math.BigDecimal;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableType;
import net.aincraft.job.model.PayableRecord;
import net.aincraft.registry.Registry;
import net.aincraft.util.Mapper;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

final class PayableRecordMapperImpl implements Mapper<Payable, PayableRecord> {

  private final Registry<PayableType> payableTypeRegistry;

  @Inject
  public PayableRecordMapperImpl(Registry<PayableType> payableTypeRegistry) {
    this.payableTypeRegistry = payableTypeRegistry;
  }

  @Override
  public @NotNull Payable toDomainObject(@NotNull PayableRecord record) throws IllegalArgumentException {
    NamespacedKey key = NamespacedKey.fromString(record.payableTypeKey());
    if (key == null) {
      throw new IllegalArgumentException("record maps to invalid jobKey");
    }
    PayableType type = payableTypeRegistry.getOrThrow(key);
    BigDecimal amount = record.amount();
    //TODO: include currency when we find a bridge
    return new Payable(type,PayableAmount.create(amount));
  }
}
