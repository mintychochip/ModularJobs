package dev.mintychochip.boost;

import dev.mintychochip.databag.DataBag;
import dev.mintychochip.databag.DataHandlers;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * ModularJobs keys and handlers on {@link DataBag}. Registers the boost-payload codec; snapshot
 * extras (job key / level) are filled at evaluation so other plugins can read the same bag without
 * a ModularJobs type.
 */
public final class ModularJobsBags {

  public static final Key JOB = Key.key("modularjobs", "job");
  public static final Key JOB_LEVEL = Key.key("modularjobs", "job_level");

  private ModularJobsBags() {}

  /** Idempotent: safe on plugin reload (same handler instance). */
  public static void register() {
    DataHandlers.register(BoostPayloadHandler.INSTANCE);
  }

  /** Extras. */
  public static DataBag extras(@Nullable String jobKey, int jobLevel) {
    DataBag bag = DataBag.create();
    if (jobKey == null || jobKey.isBlank()) {
      return bag;
    }
    return bag.setString(JOB, jobKey).setInt(JOB_LEVEL, jobLevel);
  }
}
