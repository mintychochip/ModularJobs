package net.aincraft.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.container.Context.MaterialContext;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.aincraft.service.JobService;
import net.aincraft.test.MockBukkitSupport;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Proves {@link JobsPaymentHandler} reloads progression per payable so multi-XP awards accumulate.
 */
class JobsPaymentHandlerReloadTest {

  private Plugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void reloadProgressionUsesServiceOverSnapshot() {
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    JobProgression snapshot = progression(player, BigDecimal.TEN);
    JobProgression reloaded = progression(player, new BigDecimal("99"));
    AtomicInteger getCalls = new AtomicInteger();
    JobService service = new StubJobService() {
      @Override
      public JobProgression getProgression(String playerId, String jobKey) {
        getCalls.incrementAndGet();
        return reloaded;
      }
    };
    JobsPaymentHandler handler =
        new JobsPaymentHandler(plugin, new BoostEngine(null, null, null), service);
    JobProgression result =
        handler.reloadProgression(player.getUniqueId().toString(), "modularjobs:miner", snapshot);
    assertSame(reloaded, result);
    assertEquals(1, getCalls.get());
  }

  @Test
  void payReloadsProgressionForEachPayable() {
    AtomicInteger getProgressionCalls = new AtomicInteger();
    List<BigDecimal> xpSnapshotsAtPay = new ArrayList<>();
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());

    JobProgression p0 = progression(player, new BigDecimal("0"));
    JobProgression p10 = progression(player, new BigDecimal("10"));

    PayableType expType = experienceType(ctx ->
        xpSnapshotsAtPay.add(ctx.jobProgression().experience()));

    Payable pay1 = new Payable(expType, PayableAmount.create(new BigDecimal("10"), null));
    Payable pay2 = new Payable(expType, PayableAmount.create(new BigDecimal("10"), null));
    Job job = p0.job();
    ActionType blockBreak = actionType("block_break");
    JobTask task = new JobTask(
        job.key(),
        blockBreak.key(),
        Key.key("minecraft", "stone"),
        List.of(pay1, pay2));

    JobService service = new StubJobService() {
      @Override
      public List<JobProgression> getProgressions(UUID p) {
        return List.of(p0);
      }

      @Override
      public JobTask getTask(Job j, ActionType type, Context context) {
        return task;
      }

      @Override
      public JobProgression getProgression(String playerId, String jobKey) {
        int n = getProgressionCalls.getAndIncrement();
        return n == 0 ? p0 : p10;
      }
    };

    // Offline player → BoostEngine.evaluate returns empty without needing services
    JobsPaymentHandler handler =
        new JobsPaymentHandler(plugin, new BoostEngine(null, null, null), service);
    handler.pay(player, blockBreak, new MaterialContext("minecraft:stone"));

    assertEquals(2, getProgressionCalls.get(), "one reload per payable");
    assertEquals(2, xpSnapshotsAtPay.size());
    assertEquals(0, xpSnapshotsAtPay.get(0).compareTo(BigDecimal.ZERO));
    assertEquals(0, xpSnapshotsAtPay.get(1).compareTo(new BigDecimal("10")));
  }

  private static JobProgression progression(OfflinePlayer player, BigDecimal xp) {
    Job job = new Job() {
      @Override
      public @NotNull Key key() {
        return Key.key("modularjobs", "miner");
      }

      @Override
      public @NotNull Component displayName() {
        return Component.text("Miner");
      }

      @Override
      public String getPlainName() {
        return "miner";
      }

      @Override
      public @NotNull Component description() {
        return Component.empty();
      }

      @Override
      public @NotNull LevelingCurve levelingCurve() {
        return params -> BigDecimal.valueOf(params.level() * 100L);
      }

      @Override
      public @NotNull Map<Key, PayableCurve> payableCurves() {
        return Map.of();
      }

      @Override
      public int maxLevel() {
        return 100;
      }

      @Override
      public int upgradeLevel() {
        return 0;
      }

      @Override
      public @NotNull Map<Integer, List<String>> perkUnlocks() {
        return Map.of();
      }

      @Override
      public @NotNull Map<String, Map<Integer, List<String>>> petPerks() {
        return Map.of();
      }

      @Override
      public @NotNull Map<String, List<String>> petRevokedPerks() {
        return Map.of();
      }
    };
    return new JobProgression() {
      @Override
      public BigDecimal experienceForLevel(int level) {
        return BigDecimal.valueOf(level * 100L);
      }

      @Override
      public Job job() {
        return job;
      }

      @Override
      public UUID playerId() {
        return player.getUniqueId();
      }

      @Override
      public BigDecimal experience() {
        return xp;
      }

      @Override
      public int level() {
        return 1;
      }

      @Override
      public JobProgression setExperience(BigDecimal experience) {
        return progression(player, experience);
      }
    };
  }

  private static ActionType actionType(String name) {
    Key key = Key.key("modularjobs", name);
    return new ActionType() {
      @Override
      public Key key() {
        return key;
      }

      @Override
      public String name() {
        return name;
      }
    };
  }

  private static PayableType experienceType(PayableHandler handler) {
    return new PayableType() {
      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return Key.key("modularjobs", "experience");
      }

      @Override
      public Component render(PayableAmount amount, int places) {
        return Component.empty();
      }
    };
  }

  private abstract static class StubJobService implements JobService {
    @Override
    public @NotNull List<Job> getJobs() {
      return List.of();
    }

    @Override
    public Job getJob(String jobKey) {
      return null;
    }

    @Override
    public JobTask getTask(Job job, ActionType type, Context context) {
      return null;
    }

    @Override
    public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
      return Map.of();
    }

    @Override
    public boolean update(JobProgression progression) {
      return true;
    }

    @Override
    public boolean joinJob(String playerId, String jobKey) {
      return false;
    }

    @Override
    public boolean leaveJob(String playerId, String jobKey) {
      return false;
    }

    @Override
    public JobProgression getProgression(String playerId, String jobKey) {
      return null;
    }

    @Override
    public List<JobProgression> getProgressions(UUID playerId) {
      return List.of();
    }

    @Override
    public List<JobProgression> getProgressions(Key jobKey, int limit) {
      return List.of();
    }

    @Override
    public List<JobProgression> getArchivedProgressions(UUID playerId) {
      return List.of();
    }
  }
}
