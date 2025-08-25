package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.List;
import net.aincraft.JobTask;
import net.aincraft.container.Payable;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.util.Mapper;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

final class JobTaskRecordMapperImpl implements Mapper<JobTask, JobTaskRecord> {

  private final Mapper<Payable, PayableRecord> payableRecordMapper;

  @Inject
  public JobTaskRecordMapperImpl(Mapper<Payable, PayableRecord> payableRecordMapper) {
    this.payableRecordMapper = payableRecordMapper;
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
        return  record.payables().stream().map(payableRecordMapper::toDomainObject).toList();
      }
    };
  }
}
