package net.aincraft.job;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.repository.ConnectionSource;

final class RelationalPayableRecordRepositoryImpl implements PayableRecordRepository {

  private final ConnectionSource connectionSource;

  private final Cache<CompositeKey, PayableRecord> readCache = Caffeine.newBuilder()
      .expireAfterAccess(
          Duration.ofMinutes(5)).maximumSize(1_000).build();

  private record CompositeKey(String jobKey, String actionTypeKey, String contextKey,
                              String payableTypeKey) {

  }

  RelationalPayableRecordRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public List<PayableRecord> getPayableRecords(String jobKey, String actionTypeKey,
      String contextKey) {
    List<PayableRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT payable_type_key,amount,currency FROM payable_records WHERE job_key=? AND action_type_key=? AND context_key=?;")) {
      ps.setString(1, jobKey);
      ps.setString(2, actionTypeKey);
      ps.setString(3, contextKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String payableTypeKey = rs.getString("payable_type_key");
          BigDecimal amount = rs.getBigDecimal("amount");
          String currency = rs.getString("currency");
          records.add(new PayableRecord(jobKey, actionTypeKey, contextKey, payableTypeKey, amount,
              currency));
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
