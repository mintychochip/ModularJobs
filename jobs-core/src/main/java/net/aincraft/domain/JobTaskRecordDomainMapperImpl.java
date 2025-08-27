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
  public @NotNull JobTask toDomain(@NotNull JobTaskRecord record)
      throws IllegalArgumentException {
    Key jobKey = NamespacedKey.fromString(record.jobKey());
    Key actionTypeKey = NamespacedKey.fromString(record.actionTypeKey());
    Key contextKey = NamespacedKey.fromString(record.contextKey());
    return new JobTask(jobKey, actionTypeKey, contextKey,
        record.payables().stream().map(payableRecordDomainMapper::toDomain)
            .toList());
  }

  @Override
  public @NotNull JobTaskRecord toRecord(@NotNull JobTask domain) {
    String jobKey = domain.jobKey().toString();
    String actionTypeKey = domain.actionTypeKey().toString();
    String contextKey = domain.contextKey().toString();
    return new JobTaskRecord(jobKey, actionTypeKey,
        contextKey, domain.payables().stream()
        .map(payableRecordDomainMapper::toRecord)
        .toList());
  }
}
