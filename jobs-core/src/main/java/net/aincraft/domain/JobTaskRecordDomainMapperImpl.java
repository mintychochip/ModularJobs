package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.List;
import net.aincraft.JobTask;
import net.aincraft.container.Payable;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.util.DomainMapper;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

final class JobTaskRecordDomainMapperImpl implements DomainMapper<JobTask, JobTaskRecord> {

  private final DomainMapper<Payable, PayableRecord> payableRecordDomainMapper;

  @Inject
  public JobTaskRecordDomainMapperImpl(
      DomainMapper<Payable, PayableRecord> payableRecordDomainMapper) {
    this.payableRecordDomainMapper = payableRecordDomainMapper;
  }

  @Override
  public @NotNull JobTask toDomainObject(@NotNull JobTaskRecord record)
      throws IllegalArgumentException {
    return new JobTask() {
      @Override
      public Key getContext() {
        return NamespacedKey.fromString(record.contextKey());
      }

      @Override
      public @NotNull List<Payable> getPayables() {
        return  record.payables().stream().map(payableRecordDomainMapper::toDomainObject).toList();
      }
    };
  }
}
