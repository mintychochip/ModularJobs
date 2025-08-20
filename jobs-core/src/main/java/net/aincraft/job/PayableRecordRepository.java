package net.aincraft.job;

import java.math.BigDecimal;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface PayableRecordRepository {

  List<PayableRecord> getPayableRecords(String jobKey, String actionTypeKey, String contextKey);

  record PayableRecord(String jobKey, String actionTypeKey, String contextKey,
                       String payableTypeKey, BigDecimal amount, String currencyIdentifier) {

  }
}
