package net.aincraft.gui;

import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.ItemSpec;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.JobProgression;
import net.aincraft.gui.craftux.CraftuxItems;
import net.aincraft.gui.craftux.CraftuxUiHost;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Job statistics inventory view via craftux (replaces Paper {@code Dialog} UI).
 */
public final class StatsGui {

  private static final int JOBS_PER_PAGE = 5;
  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_stats";
  private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

  private final InventoryRuntime inventory;
  private final Map<UUID, Session> sessions = new HashMap<>();

  private record Session(OfflinePlayer target, List<JobProgression> progressions, int page) {}

  /** Creates a statistics view renderer backed by the shared inventory runtime. */
  public StatsGui(InventoryRuntime inventory) {
    this.inventory = inventory;
  }

  /** Calculates the number of pages required for the supplied progression list. */
  public static int calculateTotalPages(List<JobProgression> progressions) {
    return Math.max(1, (int) Math.ceil((double) progressions.size() / JOBS_PER_PAGE));
  }

  /**
   * Stores the viewer's current target and page, then opens the corresponding view.
   *
   * <p>This method is expected on the Bukkit thread because it opens a player inventory.
   */

  public void open(Player viewer, OfflinePlayer target, List<JobProgression> progressions, int page) {
    int totalPages = calculateTotalPages(progressions);
    int safePage = Math.max(1, Math.min(page, totalPages));
    sessions.put(viewer.getUniqueId(), new Session(target, List.copyOf(progressions), safePage));
    inventory.open(viewer.getUniqueId(), buildView(viewer.getUniqueId()));
  }

  /** Host action handler for {@link CraftuxUiHost#ACTION_STATS_PREV}: open previous page. */
  public void onPrev(UUID audience, InventoryClick click) {
    Session session = sessions.get(audience);
    Player player = Bukkit.getPlayer(audience);
    if (session == null || player == null || session.page() <= 1) {
      return;
    }
    open(player, session.target(), session.progressions(), session.page() - 1);
  }

  public void onNext(UUID audience, InventoryClick click) {
    Session session = sessions.get(audience);
    Player player = Bukkit.getPlayer(audience);
    if (session == null || player == null) {
      return;
    }
    int total = calculateTotalPages(session.progressions());
    if (session.page() >= total) {
      return;
    }
    open(player, session.target(), session.progressions(), session.page() + 1);
  }

  InventoryView buildView(UUID audience) {
    Session session = sessions.get(audience);
    if (session == null) {
      return InventoryView.builder(MENU_ID, GUI_ROWS)
          .title("Job Statistics")
          .decorative(0, CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE))
          .build();
    }

    OfflinePlayer target = session.target();
    List<JobProgression> progressions = session.progressions();
    int page = session.page();
    int totalPages = calculateTotalPages(progressions);
    String targetName = target.getName() != null ? target.getName() : "Unknown";

    String title = "Stats: " + targetName + " (" + page + "/" + totalPages + ")";
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }

    Map<Integer, Slot> content = new HashMap<>();
    ItemSpec pane = CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      content.put(i, Slot.decorative(pane));
    }

    content.put(4, Slot.decorative(CraftuxItems.of(
        Material.BOOK,
        "Job Statistics",
        List.of("Player: " + targetName, "Jobs: " + progressions.size()))));

    int start = (page - 1) * JOBS_PER_PAGE;
    int end = Math.min(start + JOBS_PER_PAGE, progressions.size());
    int[] slots = {19, 20, 21, 22, 23};
    for (int i = start; i < end; i++) {
      JobProgression prog = progressions.get(i);
      int slot = slots[i - start];
      content.put(slot, Slot.decorative(jobItem(prog)));
    }

    if (page > 1) {
      content.put(45, Slot.navigation(
          "stats_prev",
          CraftuxItems.of(Material.ARROW, "Previous", List.of("Page " + (page - 1))),
          CraftuxUiHost.ACTION_STATS_PREV,
          SlotPixelIntent.UNVALIDATED));
    }
    content.put(49, Slot.decorative(CraftuxItems.of(
        Material.PAPER, "Page " + page + "/" + totalPages, List.of())));
    if (page < totalPages) {
      content.put(53, Slot.navigation(
          "stats_next",
          CraftuxItems.of(Material.ARROW, "Next", List.of("Page " + (page + 1))),
          CraftuxUiHost.ACTION_STATS_NEXT,
          SlotPixelIntent.UNVALIDATED));
    }

    InventoryView.Builder builder = InventoryView.builder(MENU_ID, GUI_ROWS).title(title);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      builder.slot(i, content.get(i));
    }
    return builder.build();
  }

  private ItemSpec jobItem(JobProgression prog) {
    int level = prog.level();
    BigDecimal xp = prog.experience();
    List<String> lore = new ArrayList<>();
    lore.add("Level: " + level);
    lore.add("XP: " + xp.setScale(1, RoundingMode.HALF_UP).toPlainString());
    try {
      BigDecimal next = prog.job().levelingCurve()
          .evaluate(new net.aincraft.LevelingCurve.Parameters(level + 1));
      lore.add("Next level: " + next.setScale(1, RoundingMode.HALF_UP).toPlainString());
    } catch (IllegalArgumentException | ArithmeticException ignored) {
      // curve may not support level+1
    }
    String name = PLAIN.serialize(prog.job().displayName());
    return CraftuxItems.of(Material.EMERALD, name, lore);
  }
}
