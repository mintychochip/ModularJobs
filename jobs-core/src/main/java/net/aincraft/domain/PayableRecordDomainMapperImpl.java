package net.aincraft.domain;

import com.google.inject.Inject;
import java.math.BigDecimal;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.registry.Registry;
import net.aincraft.util.DomainMapper;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

final class PayableRecordDomainMapperImpl implements DomainMapper<Payable, PayableRecord> {

  private final Registry<PayableType> payableTypeRegistry;

  @Inject
  public PayableRecordDomainMapperImpl(Registry<PayableType> payableTypeRegistry) {
    this.payableTypeRegistry = payableTypeRegistry;
  }

  @Override
  public @NotNull Payable toDomain(@NotNull PayableRecord record) throws IllegalArgumentException {
    NamespacedKey key = NamespacedKey.fromString(record.payableTypeKey());
    if (key == null) {
      throw new IllegalArgumentException("record maps to invalid jobKey");
    }
    PayableType type = payableTypeRegistry.getOrThrow(key);

    BigDecimal amount = record.amount();
    //TODO: include currencyIdentifier when we find a bridge
    return new Payable(type, PayableAmount.create(amount));
  }

  @Override
  public @NotNull PayableRecord toRecord(@NotNull Payable domain) {
    PayableType type = domain.type();
    PayableAmount amount = domain.amount();
    return new PayableRecord(type.key().toString(), amount.value(),
        amount.currency().get().identifier());
  }
}
