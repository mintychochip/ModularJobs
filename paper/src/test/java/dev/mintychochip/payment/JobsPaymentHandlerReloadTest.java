package dev.mintychochip.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.container.Context.MaterialContext;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.profession.RecipeExperienceDepreciationPolicy;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import java.util.OptionalInt;
import dev.mintychochip.test.MockBukkitSupport;
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


  @Test
  void payAppliesRecipeDepreciationForCraftExperience() {
    List<BigDecimal> paid = new ArrayList<>();
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    Key recipeId = Key.key("modularjobs", "masterwork_iron_sword");
    Key output = Key.key("minecraft", "iron_sword");

    RecordingRecipeService recipes = new RecordingRecipeService();
    recipes.registerDefinition(new RecipeDefinition(recipeId, "weaponsmithing", 25, 2, output));

    PayableType expType = experienceType(ctx -> paid.add(ctx.payable().amount().value()));
    Payable payable = new Payable(expType, PayableAmount.create(new BigDecimal("100"), null));
    JobProgression progression = progression(player, BigDecimal.ZERO);
    ActionType craft = actionType("craft");
    JobTask task = new JobTask(
        progression.job().key(),
        craft.key(),
        Key.key("minecraft", "iron_sword"),
        List.of(payable));

    JobService service = new StubJobService() {
      @Override
      public List<JobProgression> getProgressions(UUID p) {
        return List.of(progression);
      }

      @Override
      public JobTask getTask(Job job, ActionType type, Context context) {
        return task;
      }
    };

    RecipeExperienceDepreciationApplier depreciation =
        new RecipeExperienceDepreciationApplier(
            new RecipeExperienceDepreciationPolicy(true, 0, 10),
            recipes,
            new RecordingProfessionService(30));

    JobsPaymentHandler handler =
        new JobsPaymentHandler(plugin, new BoostEngine(null, null, null), service, depreciation);
    handler.pay(player, craft, new Context.ItemContext("minecraft:iron_sword", 1));

    assertEquals(output, recipes.lastCraftOutputLookup());
    assertEquals(1, paid.size());
    assertEquals(0, new BigDecimal("50").compareTo(paid.get(0)));
  }

  private static final class RecordingRecipeService implements RecipeService {
    private final java.util.Map<Key, RecipeDefinition> byCraftOutput = new java.util.HashMap<>();
    private Key lastCraftOutputLookup;

    @Override
    public boolean knows(UUID playerId, Key recipeId) {
      return false;
    }

    @Override
    public void grant(UUID playerId, Key recipeId) {}

    @Override
    public void revoke(UUID playerId, Key recipeId) {}

    @Override
    public java.util.Set<Key> knownRecipes(UUID playerId) {
      return java.util.Set.of();
    }

    @Override
    public boolean canCraft(UUID playerId, Key recipeId, int professionLevel) {
      return false;
    }

    @Override
    public void registerDefinition(RecipeDefinition definition) {
      byCraftOutput.put(definition.craftOutputKey(), definition);
    }

    @Override
    public java.util.Optional<RecipeDefinition> definition(Key recipeId) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<RecipeDefinition> definitionForCraftOutput(Key outputMaterialKey) {
      lastCraftOutputLookup = outputMaterialKey;
      return java.util.Optional.ofNullable(byCraftOutput.get(outputMaterialKey));
    }

    Key lastCraftOutputLookup() {
      return lastCraftOutputLookup;
    }
  }

  private static final class RecordingProfessionService implements ProfessionService {
    private final int level;

    RecordingProfessionService(int level) {
      this.level = level;
    }

    @Override
    public java.util.List<dev.mintychochip.profession.ProfessionDefinition> tracks() {
      return java.util.List.of();
    }

    @Override
    public java.util.Optional<dev.mintychochip.profession.ProfessionDefinition> resolve(String idOrAlias) {
      return java.util.Optional.empty();
    }

    @Override
    public OptionalInt level(UUID playerId, String professionIdOrAlias) {
      return OptionalInt.of(level);
    }

    @Override
    public java.util.Optional<BigDecimal> experience(UUID playerId, String professionIdOrAlias) {
      return java.util.Optional.empty();
    }

    @Override
    public boolean ensureTrack(UUID playerId, String professionIdOrAlias) {
      return false;
    }
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
