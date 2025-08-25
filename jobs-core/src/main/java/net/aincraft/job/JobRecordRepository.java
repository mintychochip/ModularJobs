package net.aincraft.job;

import java.util.List;
import net.aincraft.domain.model.JobRecord;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
public interface JobRecordRepository {

  @NotNull
  List<JobRecord> getJobs();
  @Nullable
  JobRecord getJob(String jobKey);

}
