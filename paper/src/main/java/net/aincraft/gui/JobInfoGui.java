package net.aincraft.gui;

import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.ItemSpec;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.gui.craftux.CraftuxItems;
import net.aincraft.gui.craftux.CraftuxUiHost;
import net.aincraft.service.PreferencesService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Job info inventory (action types + rewards) via craftux.
 *
 * <p>Replaces the Paper Dialog path previously used by {@code /jobs info [gui]}.
 */
public final class JobInfoGui {

  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_info";
  private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

  private final InventoryRuntime inventory;
  private final PreferencesService preferencesService;
  private final Map<UUID, Session> sessions = new HashMap<>();

  private record Session(
      Job job,
      List<Map.Entry<ActionType, List<JobTask>>> entries,
      int page,
      int entriesPerPage) {}

  /** Builds the job-info presenter over the shared craftux runtime. */
  public JobInfoGui(InventoryRuntime inventory, PreferencesService preferencesService) {
    this.inventory = inventory;
    this.preferencesService = preferencesService;
  }

  /** Calculates the number of pages required for the supplied task groups. */
  public int calculateTotalPages(Map<ActionType, List<JobTask>> tasks, int entriesPerPage) {
    return Math.max(1, (int) Math.ceil((double) tasks.size() / Math.max(1, entriesPerPage)));
  }

  /**
   * Opens the job-info inventory for {@code player}.
   *
   * @return {@code true} when the page was valid and the view opened
   */
  public boolean open(Player player, Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    int entriesPerPage = preferencesService.getEntriesPerPage(player.getUniqueId());
    int totalPages = calculateTotalPages(tasks, entriesPerPage);
    if (page < 1 || page > totalPages) {
      return false;
    }
    List<Map.Entry<ActionType, List<JobTask>>> entries = new ArrayList<>(tasks.entrySet());
    sessions.put(player.getUniqueId(), new Session(job, entries, page, entriesPerPage));
    inventory.open(player.getUniqueId(), buildView(player.getUniqueId()));
    return true;
  }

  /** Host action handler for {@link CraftuxUiHost#ACTION_INFO_PREV}: open previous page. */
  public void onPrev(UUID audience, InventoryClick click) {
    Session session = sessions.get(audience);
    Player player = Bukkit.getPlayer(audience);
    if (session == null || player == null || session.page() <= 1) {
      return;
    }
    reopen(player, session.page() - 1);
  }

  /** Host action handler for {@link CraftuxUiHost#ACTION_INFO_NEXT}: open next page. */
  public void onNext(UUID audience, InventoryClick click) {
    Session session = sessions.get(audience);
    Player player = Bukkit.getPlayer(audience);
    if (session == null || player == null) {
      return;
    }
    int total = Math.max(1, (int) Math.ceil(
        (double) session.entries().size() / Math.max(1, session.entriesPerPage())));
    if (session.page() >= total) {
      return;
    }
    reopen(player, session.page() + 1);
  }

  private void reopen(Player player, int page) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    Map<ActionType, List<JobTask>> map = new java.util.LinkedHashMap<>();
    for (Map.Entry<ActionType, List<JobTask>> e : session.entries()) {
      map.put(e.getKey(), e.getValue());
    }
    open(player, session.job(), map, page);
  }

  InventoryView buildView(UUID audience) {
    Session session = sessions.get(audience);
    if (session == null) {
      return InventoryView.builder(MENU_ID, GUI_ROWS)
          .title("Job Info")
          .decorative(0, CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE))
          .build();
    }

    Job job = session.job();
    int page = session.page();
    int entriesPerPage = session.entriesPerPage();
    List<Map.Entry<ActionType, List<JobTask>>> entries = session.entries();
    int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / Math.max(1, entriesPerPage)));

    String jobName = PLAIN.serialize(job.displayName());
    String title = "Info: " + jobName + " (" + page + "/" + totalPages + ")";
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }

    Map<Integer, Slot> content = new HashMap<>();
    ItemSpec pane = CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      content.put(i, Slot.decorative(pane));
    }

    List<String> headerLore = new ArrayList<>();
    headerLore.add(PLAIN.serialize(job.description()));
    headerLore.add("Max Level: " + job.maxLevel());
    content.put(4, Slot.decorative(CraftuxItems.of(Material.BOOK, jobName, headerLore)));

    int start = (page - 1) * entriesPerPage;
    int end = Math.min(start + entriesPerPage, entries.size());
    // Content slots: rows 1-4, columns 1-7 (slots 10-16, 19-25, 28-34, 37-43)
    int[] contentSlots = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < contentSlots.length; i++) {
      Map.Entry<ActionType, List<JobTask>> entry = entries.get(i);
      if (entry.getValue().isEmpty()) {
        continue;
      }
      content.put(contentSlots[slotIndex], Slot.decorative(actionItem(entry.getKey(), entry.getValue())));
      slotIndex++;
    }

    if (page > 1) {
      content.put(45, Slot.navigation(
          "info_prev",
          CraftuxItems.of(Material.ARROW, "Previous", List.of("Page " + (page - 1))),
          CraftuxUiHost.ACTION_INFO_PREV,
          SlotPixelIntent.UNVALIDATED));
    }
    content.put(49, Slot.decorative(CraftuxItems.of(
        Material.PAPER, "Page " + page + "/" + totalPages, List.of())));
    if (page < totalPages) {
      content.put(53, Slot.navigation(
          "info_next",
          CraftuxItems.of(Material.ARROW, "Next", List.of("Page " + (page + 1))),
          CraftuxUiHost.ACTION_INFO_NEXT,
          SlotPixelIntent.UNVALIDATED));
    }

    InventoryView.Builder builder = InventoryView.builder(MENU_ID, GUI_ROWS).title(title);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      builder.slot(i, content.get(i));
    }
    return builder.build();
  }

  private ItemSpec actionItem(ActionType type, List<JobTask> tasks) {
    String name = formatActionTypeName(type.name());
    List<String> lore = new ArrayList<>();
    int shown = 0;
    for (JobTask task : tasks) {
      if (shown >= 8) {
        lore.add("… +" + (tasks.size() - shown) + " more");
        break;
      }
      lore.add(formatContextKey(task.contextKey()) + " → " + formatPayables(task.payables()));
      shown++;
    }
    return CraftuxItems.of(Material.PAPER, name, lore);
  }

  private static String formatPayables(List<Payable> payables) {
    if (payables.isEmpty()) {
      return "No rewards";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < payables.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      // Payable.asComponent() may include MiniMessage; use plain type key + amount
      Payable p = payables.get(i);
      sb.append(PLAIN.serialize(p.asComponent()));
    }
    return sb.toString();
  }

  private static String formatActionTypeName(String name) {
    return Arrays.stream(name.toLowerCase(java.util.Locale.ROOT).split("_"))
        .filter(w -> !w.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  private static String formatContextKey(Key key) {
    String value = key.value();
    return Arrays.stream(value.split("[_/]"))
        .filter(w -> !w.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }
}
