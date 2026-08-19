package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.config.ProgressionLimitsConfig;
import net.aincraft.service.JoinGate.JoinResult;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class JoinGateTest {

  private static final Set<String> NO_DISABLED_WORLDS = Set.of();

  @Test
  void allowsWhenUnderLimitAndPermitted() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(2, List.of(), true), NO_DISABLED_WORLDS);
    assertEquals(JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of()));
  }

  @Test
  void deniesWhenAtMaxJobs() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(1, List.of(), true), NO_DISABLED_WORLDS);
    JobProgression existing = progression(job("farmer"));
    assertEquals(JoinResult.MAX_JOBS,
        gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of(existing)));
  }

  @Test
  void deniesWhenPerJobPermissionMissing() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(5, List.of(), true), NO_DISABLED_WORLDS);
    assertEquals(JoinResult.PERMISSION_DENIED,
        gate.canJoin(permittedPlayer(false, "world"), job("miner"), List.of()));
  }

  @Test
  void unlimitedMaxJobsDoesNotGate() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(0, List.of(), true), NO_DISABLED_WORLDS);
    JobProgression a = progression(job("farmer"));
    JobProgression b = progression(job("builder"));
    assertEquals(JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of(a, b)));
  }

  @Test
  void permissionCheckLowercasesJobName() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(5, List.of(), true), NO_DISABLED_WORLDS);
    assertEquals(JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true, "world"), job("Miner"), List.of()));
  }

  @Test
  void worldRestrictionRejectsDisabledWorldCaseInsensitively() {
    JoinGate gate = new JoinGate(
        new ProgressionLimitsConfig(5, List.of(), true), Set.of("nether"));
    assertEquals(JoinResult.WORLD_DENIED,
        gate.canJoin(permittedPlayer(true, "NETHER"), job("miner"), List.of()));
  }

  @Test
  void worldRestrictionAllowsNonDisabledWorld() {
    JoinGate gate = new JoinGate(
        new ProgressionLimitsConfig(5, List.of(), true), Set.of("nether"));
    assertEquals(JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of()));
  }

  @Test
  void worldRestrictionDisabledBypassesWorldList() {
    JoinGate gate = new JoinGate(
        new ProgressionLimitsConfig(5, List.of(), false), Set.of("nether"));
    assertEquals(JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true, "NETHER"), job("miner"), List.of()));
  }

  @Test
  void defaultsLocaleRoundTrip() {
    // Guards against accidental locale-dependent permission building.
    assertEquals("jobs.join.miner", "jobs.join." + "Miner".toLowerCase(Locale.ROOT));
  }

  private static Player permittedPlayer(boolean permitted, String worldName) {
    return (Player) java.lang.reflect.Proxy.newProxyInstance(
        Player.class.getClassLoader(), new Class<?>[] {Player.class},
        (proxy, method, args) -> {
          if (method.getName().equals("hasPermission")) return permitted;
          if (method.getName().equals("getWorld")) return world(worldName);
          return defaultValue(method.getReturnType());
        });
  }

  private static Object world(String name) {
    return java.lang.reflect.Proxy.newProxyInstance(
        org.bukkit.World.class.getClassLoader(),
        new Class<?>[] {org.bukkit.World.class},
        (proxy, method, args) ->
            method.getName().equals("getName") ? name : defaultValue(method.getReturnType()));
  }

  private static Job job(String name) {
    return (Job) java.lang.reflect.Proxy.newProxyInstance(
        Job.class.getClassLoader(), new Class<?>[] {Job.class},
        (proxy, method, args) -> {
          if (method.getName().equals("getPlainName")) return name;
          if (method.getName().equals("key")) {
            return net.kyori.adventure.key.Key.key(
                "modularjobs", name.toLowerCase(java.util.Locale.ROOT));
          }
          return defaultValue(method.getReturnType());
        });
  }

  private static JobProgression progression(Job job) {
    return (JobProgression) java.lang.reflect.Proxy.newProxyInstance(
        JobProgression.class.getClassLoader(), new Class<?>[] {JobProgression.class},
        (proxy, method, args) ->
            method.getName().equals("job") ? job : defaultValue(method.getReturnType()));
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == double.class) return 0D;
    return null;
  }
}
