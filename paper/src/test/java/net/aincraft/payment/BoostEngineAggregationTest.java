package net.aincraft.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import java.sql.Connection;
import java.sql.SQLException;
import net.aincraft.registry.SimpleRegistryImpl;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.repository.DatabaseType;
import net.aincraft.boost.BoostDataCodec;
import net.aincraft.boost.BoostFactoryImpl;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import net.aincraft.service.ItemBoostDataService;
import net.aincraft.upgrade.PlayerUpgradeRepository;
import net.aincraft.upgrade.SkillTree;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.aincraft.upgrade.UpgradeBoostDataServiceImpl;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link BoostEngine#evaluateSources} + {@link BoostEngine#applyBoosts}
 * with controlled item/timed/upgrade sources (fakes for deps only).
 */
class BoostEngineAggregationTest {

  private static final Condition ALWAYS = ctx -> true;

  @Test
  void aggregatesItemTimedAndUpgradeSources() {
    BoostSource itemSource = ruled(
        Key.key("modularjobs", "item_src"),
        new MultiplicativeBoostImpl(new BigDecimal("2.0"))
    );
    BoostSource timedSource = ruled(
        Key.key("modularjobs", "timed_src"),
        new MultiplicativeBoostImpl(new BigDecimal("1.5"))
    );
    BoostSource upgradeSource = ruled(
        Key.key("modularjobs", "upgrade_src"),
        new AdditiveBoostImpl(new BigDecimal("10"))
    );

    BoostEngine engine = new BoostEngine(
        unusedItemService(),
        unusedTimedService(),
        unusedUpgradeService()
    );

    BoostContext context = new BoostContext(null, null, null, null, null);
    ActiveBoostData timed = new ActiveBoostData(
        UUID.randomUUID().toString(),
        timedSource.key().toString(),
        Instant.now(),
        Duration.ofHours(1),
        timedSource
    );

    Map<Key, Boost> boosts = engine.evaluateSources(
        context,
        List.of(itemSource),
        List.of(timed),
        List.of(upgradeSource)
    );

    assertEquals(3, boosts.size());
    assertTrue(boosts.containsKey(itemSource.key()));
    assertTrue(boosts.containsKey(timedSource.key()));
    assertTrue(boosts.containsKey(upgradeSource.key()));

    BigDecimal base = new BigDecimal("100");
    BigDecimal deterministic = applyInKeyOrder(base, boosts, List.of(
        itemSource.key(), timedSource.key(), upgradeSource.key()));
    // 100 * 2 * 1.5 + 10 = 310
    assertEquals(0, new BigDecimal("310.0").compareTo(deterministic),
        "100 * 2 * 1.5 + 10 = 310, got " + deterministic);

    BigDecimal applied = BoostEngine.applyBoosts(base, boosts);
    assertTrue(applied.compareTo(base) != 0, "payment path must change base amount");

    assertEquals(0, new BigDecimal("200").compareTo(boosts.get(itemSource.key()).boost(base)));
    assertEquals(0, new BigDecimal("150.0").compareTo(boosts.get(timedSource.key()).boost(base)));
    assertEquals(0, new BigDecimal("110").compareTo(boosts.get(upgradeSource.key()).boost(base)));
  }

  @Test
  void multiplicativeAndAdditiveMathOnRealBoostImpls() {
    Boost multi = new MultiplicativeBoostImpl(new BigDecimal("1.25"));
    Boost add = new AdditiveBoostImpl(new BigDecimal("25"));
    Map<Key, Boost> boosts = Map.of(
        Key.key("a", "multi"), multi,
        Key.key("a", "add"), add
    );

    BigDecimal multiFirst = add.boost(multi.boost(new BigDecimal("100")));
    BigDecimal addFirst = multi.boost(add.boost(new BigDecimal("100")));
    assertEquals(0, new BigDecimal("150.00").compareTo(multiFirst));
    assertEquals(0, new BigDecimal("156.25").compareTo(addFirst));

    BigDecimal applied = BoostEngine.applyBoosts(new BigDecimal("100"), boosts);
    assertTrue(
        applied.compareTo(multiFirst) == 0 || applied.compareTo(addFirst) == 0,
        "applyBoosts must apply both boosts, got " + applied
    );
  }

  private static BigDecimal applyInKeyOrder(
      BigDecimal base, Map<Key, Boost> boosts, List<Key> order) {
    BigDecimal current = base;
    for (Key key : order) {
      current = boosts.get(key).boost(current);
    }
    return current;
  }

  private static BoostSource ruled(Key key, Boost boost) {
    return new RuledBoostSourceImpl(
        List.of(new Rule(ALWAYS, 1, boost)),
        key,
        "test"
    );
  }

  private static ItemBoostDataService unusedItemService() {
    // evaluateSources does not call item service; real concrete still wires BoostEngine
    return new ItemBoostDataService(
        new BoostDataCodec(GsonConditionSerializer.gson(), BoostFactoryImpl.INSTANCE));
  }

  private static TimedBoostDataService unusedTimedService() {
    return new TimedBoostDataService() {
      @Override
      public List<ActiveBoostData> findApplicableBoosts(Target target) {
        return List.of();
      }

      @Override
      public List<ActiveBoostData> findBoosts(Target target) {
        return List.of();
      }

      @Override
      public <T extends net.aincraft.container.boost.BoostData.TimedBoostData
          & SerializableBoostData> void addData(T data, Target target) {
      }

      @Override
      public boolean removeBoost(Target target, String sourceIdentifier) {
        return false;
      }
    };
  }

  private static UpgradeBoostDataService unusedUpgradeService() {
    // Connection never used on evaluateSources path
    return new UpgradeBoostDataServiceImpl(
        new PlayerUpgradeRepository(unusedConnectionSource()),
        new SimpleRegistryImpl<>(),
        new SimpleRegistryImpl<SkillTree>());
  }

  private static ConnectionSource unusedConnectionSource() {
    return new ConnectionSource() {
      @Override
      public void shutdown() {
      }

      @Override
      public DatabaseType getType() {
        return DatabaseType.MYSQL;
      }

      @Override
      public boolean isClosed() {
        return false;
      }

      @Override
      public Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException("unused in evaluateSources test");
      }

      @Override
      public boolean isSetup() {
        return true;
      }
    };
  }
}
