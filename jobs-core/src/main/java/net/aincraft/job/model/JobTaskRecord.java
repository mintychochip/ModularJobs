package net.aincraft.job.model;

import java.util.List;

public record JobTaskRecord(String jobKey, String actionTypeKey, String contextKey,
                            List<PayableRecord> payables) {

}
