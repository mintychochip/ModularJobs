package net.aincraft.job;

import java.util.Map;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
public interface JobRecordRepository {

  @Nullable
  JobRecord getJob(String jobKey);

  record JobRecord(@NotNull String jobKey, @NotNull String displayName,
                   @Nullable String description, int maxLevel,
                   @NotNull String levellingCurve, @NotNull Map<String, String> payableCurves) {

  }

}
