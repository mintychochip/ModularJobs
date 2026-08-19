package net.aincraft.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ModularJobsPlaceholderExpansionTest {

  private static final UUID PLAYER = UUID.randomUUID();

  @Test
  void exposesLevelAndExperiencePerJob() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("7", expansion.onRequest(offlinePlayer(), "level_miner"));
    assertEquals("150", expansion.onRequest(offlinePlayer(), "experience_miner"));
  }

  @Test
  void exposesJoinedJobCountAndJobList() {
    JobService service = serviceWith(progression("miner", 7, "150"), progression("farmer", 3, "50"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("2", expansion.onRequest(offlinePlayer(), "joinedjobcount"));
    assertEquals("miner,farmer", expansion.onRequest(offlinePlayer(), "jobs"));
  }

  @Test
  void exposesTotalLevels() {
    JobService service = serviceWith(progression("miner", 7, "150"), progression("farmer", 3, "50"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("10", expansion.onRequest(offlinePlayer(), "totallevels"));
  }

  @Test
  void exposesJobNameDescriptionAndMaxLevel() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("miner", expansion.onRequest(offlinePlayer(), "name_miner"));
    assertEquals("Mines ores", expansion.onRequest(offlinePlayer(), "description_miner"));
    assertEquals("100", expansion.onRequest(offlinePlayer(), "maxlevel_miner"));
  }

  @Test
  void exposesMaxExperienceForCurrentLevel() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    // experienceForLevel(8) = "800" from the proxy stub
    assertEquals("800", expansion.onRequest(offlinePlayer(), "maxexperience_miner"));
  }

  @Test
  void exposesIsinAndCanjoin() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("true", expansion.onRequest(offlinePlayer(), "isin_miner"));
    assertEquals("false", expansion.onRequest(offlinePlayer(), "isin_farmer"));
    assertEquals("false", expansion.onRequest(offlinePlayer(), "canjoin_miner"));
    assertEquals("true", expansion.onRequest(offlinePlayer(), "canjoin_farmer"));
  }

  @Test
  void exposesArchivedJobCountAndMaxJobs() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    // archivedcount proxy returns 1 via getArchivedProgressions
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("1", expansion.onRequest(offlinePlayer(), "archivedjobs"));
    assertEquals("1", expansion.onRequest(offlinePlayer(), "maxjobs"));
  }

  @Test
  void returnsEmptyForUnknownPlaceholder() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("", expansion.onRequest(offlinePlayer(), "bogus_param"));
  }

  @Test
  void missingProgressionReturnsEmpty() {
    JobService service = serviceWith();
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("", expansion.onRequest(offlinePlayer(), "level_miner"));
  }

  private static JobService serviceWith(JobProgression... progressions) {
    return (JobService) java.lang.reflect.Proxy.newProxyInstance(
        JobService.class.getClassLoader(), new Class<?>[] {JobService.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getJobs" -> {
              return jobsOf(progressions);
            }
            case "getProgressions" -> {
              return List.of(progressions);
            }
            case "getArchivedProgressions" -> {
              return List.of(progressions.length > 0 ? progressions[0] : null);
            }
            case "getProgression" -> {
              String jobKey = (String) args[1];
              for (JobProgression p : progressions) {
                if (p.job().key().toString().endsWith(jobKey)) {
                  return p;
                }
              }
              return null;
            }
            default -> {
              return defaultValue(method.getReturnType());
            }
          }
        });
  }

  private static List<Job> jobsOf(JobProgression... progressions) {
    java.util.ArrayList<Job> jobs = new java.util.ArrayList<>();
    for (JobProgression p : progressions) {
      jobs.add(p.job());
    }
    return jobs;
  }

  private static JobProgression progression(String name, int level, String exp) {
    Job job = job(name);
    return (JobProgression) java.lang.reflect.Proxy.newProxyInstance(
        JobProgression.class.getClassLoader(), new Class<?>[] {JobProgression.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "job" -> {
              return job;
            }
            case "level" -> {
              return level;
            }
            case "experience" -> {
              return new BigDecimal(exp);
            }
            case "experienceForLevel" -> {
              return new BigDecimal("100").multiply(BigDecimal.valueOf((Integer) args[0]));
            }
            default -> {
              return defaultValue(method.getReturnType());
            }
          }
        });
  }

  private static Job job(String name) {
    return (Job) java.lang.reflect.Proxy.newProxyInstance(
        Job.class.getClassLoader(), new Class<?>[] {Job.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getPlainName" -> {
              return name;
            }
            case "key" -> {
              return Key.key("modularjobs", name);
            }
            case "maxLevel" -> {
              return 100;
            }
            case "displayName" -> {
              return net.kyori.adventure.text.Component.text(name);
            }
            case "description" -> {
              return net.kyori.adventure.text.Component.text("Mines ores");
            }
            default -> {
              return defaultValue(method.getReturnType());
            }
          }
        });
  }

  private static org.bukkit.OfflinePlayer offlinePlayer() {
    return (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
        org.bukkit.OfflinePlayer.class.getClassLoader(),
        new Class<?>[] {org.bukkit.OfflinePlayer.class},
        (proxy, method, args) ->
            method.getName().equals("getUniqueId") ? PLAYER : defaultValue(method.getReturnType()));
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == double.class) return 0D;
    return null;
  }
}
