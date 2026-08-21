package dev.mintychochip.gui.craftux;

import static java.util.Objects.requireNonNull;

import dev.craftux.api.model.BossBarPlan;
import dev.craftux.api.model.RichText;
import dev.craftux.api.model.ScoreboardLine;
import dev.craftux.api.model.ScoreboardPlan;
import dev.craftux.api.model.TextSpan;
import dev.craftux.api.render.SurfaceHandle;
import dev.craftux.api.theme.StyleRole;
import dev.craftux.paper.port.BukkitPaperAudience;
import dev.craftux.paper.port.PaperAudience;
import dev.craftux.paper.port.PaperAudienceProvider;
import dev.craftux.paper.port.ToastSender;
import dev.craftux.paper.surface.BossBarRenderer;
import dev.craftux.paper.surface.ScoreboardRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Host-owned craftux text surfaces (scoreboard + boss bar) for ModularJobs.
 *
 * <p>Inventory GUIs use {@link CraftuxUiHost#inventory()}; ephemeral top
 * leaderboards and XP bars go through these Paper surface renderers.
 */
public final class CraftuxSurfaces {

  private final ScoreboardRenderer scoreboards;
  private final BossBarRenderer bossBars;
  private final Map<UUID, SurfaceHandle> scoreboardHandles = new HashMap<>();
  private final Map<String, SurfaceHandle> bossBarHandles = new HashMap<>();

  private CraftuxSurfaces(ScoreboardRenderer scoreboards, BossBarRenderer bossBars) {
    this.scoreboards = scoreboards;
    this.bossBars = bossBars;
  }

  public static CraftuxSurfaces create() {
    PaperAudienceProvider audiences = CraftuxSurfaces::audience;
    return new CraftuxSurfaces(
        new ScoreboardRenderer(audiences),
        new BossBarRenderer(audiences));
  }

  private static PaperAudience audience(UUID id) {
    Player player = Bukkit.getPlayer(id);
    return player == null ? null : BukkitPaperAudience.of(player, ToastSender.unsupported());
  }

  /**
   * Mounts or replaces the sidebar scoreboard for {@code audience} with a title
   * and body lines (max 15). Empty lines are skipped.
   */
  public void showScoreboard(UUID audience, String title, List<String> lines) {
    requireNonNull(audience, "audience");
    requireNonNull(title, "title");
    requireNonNull(lines, "lines");

    hideScoreboard(audience);

    List<ScoreboardLine> body = new ArrayList<>();
    int limit = Math.min(lines.size(), ScoreboardRenderer.MAX_LINES);
    for (int i = 0; i < limit; i++) {
      String line = lines.get(i);
      if (line == null || line.isBlank()) {
        continue;
      }
      body.add(new ScoreboardLine("line_" + i, rich(line, StyleRole.BODY)));
    }
    ScoreboardPlan plan = new ScoreboardPlan(rich(title, StyleRole.HEADING), body);
    SurfaceHandle handle = scoreboards.mount(audience, plan);
    scoreboardHandles.put(audience, handle);
  }

  /** Unmounts the craftux scoreboard for {@code audience}, if any. */
  public void hideScoreboard(UUID audience) {
    SurfaceHandle handle = scoreboardHandles.remove(requireNonNull(audience, "audience"));
    if (handle != null) {
      scoreboards.unmount(handle);
    }
  }

  /**
   * Mounts or replaces a named boss bar for {@code audience}.
   *
   * @param barKey stable key (e.g. player+job) so multiple bars can coexist
   * @param title  plain title text (MiniMessage optional via {@code miniMessage})
   * @param miniMessage optional MiniMessage markup for the title, or null
   * @param progress normalized 0..1
   * @param role semantic style (maps to Adventure bar color)
   */
  public void showBossBar(
      UUID audience,
      String barKey,
      String title,
      String miniMessage,
      double progress,
      StyleRole role) {
    requireNonNull(audience, "audience");
    requireNonNull(barKey, "barKey");
    requireNonNull(title, "title");
    requireNonNull(role, "role");

    String composite = audience + ":" + barKey;
    hideBossBar(audience, barKey);

    TextSpan span = miniMessage == null || miniMessage.isBlank()
        ? new TextSpan(title, role)
        : new TextSpan(title, role, null, null, miniMessage);
    BossBarPlan plan = new BossBarPlan(new RichText(List.of(span)), clamp(progress), role);
    SurfaceHandle handle = bossBars.mount(audience, plan);
    bossBarHandles.put(composite, handle);
  }

  /** Unmounts one named boss bar. */
  public void hideBossBar(UUID audience, String barKey) {
    String composite = requireNonNull(audience, "audience") + ":" + requireNonNull(barKey, "barKey");
    SurfaceHandle handle = bossBarHandles.remove(composite);
    if (handle != null) {
      bossBars.unmount(handle);
    }
  }

  /** Unmounts every boss bar tracked for {@code audience}. */
  public void hideAllBossBars(UUID audience) {
    requireNonNull(audience, "audience");
    String prefix = audience + ":";
    List<String> keys = new ArrayList<>();
    for (String key : bossBarHandles.keySet()) {
      if (key.startsWith(prefix)) {
        keys.add(key);
      }
    }
    for (String key : keys) {
      SurfaceHandle handle = bossBarHandles.remove(key);
      if (handle != null) {
        bossBars.unmount(handle);
      }
    }
  }

  public ScoreboardRenderer scoreboards() {
    return scoreboards;
  }

  public BossBarRenderer bossBars() {
    return bossBars;
  }

  private static RichText rich(String text, StyleRole role) {
    return new RichText(List.of(new TextSpan(text, role)));
  }

  private static double clamp(double progress) {
    if (Double.isNaN(progress)) {
      return 0.0;
    }
    if (progress < 0.0) {
      return 0.0;
    }
    if (progress > 1.0) {
      return 1.0;
    }
    return progress;
  }
}
