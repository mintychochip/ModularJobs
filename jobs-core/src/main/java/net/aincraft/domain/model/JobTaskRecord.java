package net.aincraft.domain.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record JobTaskRecord(@NotNull String jobKey, String actionTypeKey, String contextKey,
                            List<PayableRecord> payables) {

}
