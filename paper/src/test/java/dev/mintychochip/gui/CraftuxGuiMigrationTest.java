package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryClickKind;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.api.inventory.SlotRole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import dev.mintychochip.gui.craftux.CraftuxActionBus;
import dev.mintychochip.gui.craftux.CraftuxItems;
import dev.mintychochip.gui.craftux.CraftuxUiHost;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Proves ModularJobs GUI sources use craftux (not triumph-gui) and that the
 * host action bus + inventory view construction path work on the real craftux
 * types shipped into the plugin.
 */
class CraftuxGuiMigrationTest {

  @Test
  void sourceTreeHasNoTriumphGuiImports() throws Exception {
    Path paperMain = Path.of("src/main/java");
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(paperMain)) {
      files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
        try {
          String text = Files.readString(path, StandardCharsets.UTF_8);
          if (text.contains("dev.triumphteam.gui") || text.contains("triumphteam")) {
            offenders.add(path.toString());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
    assertTrue(offenders.isEmpty(), "triumph-gui remnants remain in: " + offenders);
  }

  @Test
  void guiSourcesUseCraftuxInventoryRuntime() throws Exception {
    for (String relative : List.of(
        "src/main/java/dev/mintychochip/gui/JobBrowseGui.java",
        "src/main/java/dev/mintychochip/gui/UpgradeTreeGui.java",
        "src/main/java/dev/mintychochip/gui/JobInfoGui.java",
        "src/main/java/dev/mintychochip/gui/StatsGui.java",
        "src/main/java/dev/mintychochip/upgrade/editor/TreeEditorGui.java",
        "src/main/java/dev/mintychochip/upgrade/editor/TreeEditorNodeGui.java",
        "src/main/java/dev/mintychochip/upgrade/editor/TreeEditorSettingsGui.java")) {
      String text = Files.readString(Path.of(relative), StandardCharsets.UTF_8);
      assertTrue(text.contains("dev.craftux"), relative + " must import craftux");
      assertTrue(
          text.contains("InventoryRuntime") || text.contains("InventoryView"),
          relative + " must use craftux inventory types");
      assertFalse(text.contains("dev.triumphteam"), relative + " must not use triumph-gui");
    }
  }

  @Test
  void pluginContextWiresCraftuxUiHost() throws Exception {
    String text = Files.readString(
        Path.of("src/main/java/dev/mintychochip/PluginContext.java"), StandardCharsets.UTF_8);
    assertTrue(text.contains("CraftuxUiHost"), "PluginContext must create CraftuxUiHost");
    assertTrue(text.contains("craftuxUi.actions().register"), "PluginContext must register actions");
    assertTrue(text.contains("ACTION_JOB_JOIN"), "PluginContext must wire job join action");
  }

  @Test
  void actionBusProxiesKnownIdsOnShippedPath() {
    CraftuxActionBus bus = new CraftuxActionBus(List.of(CraftuxUiHost.ACTION_JOB_JOIN));
    assertTrue(bus.proxies().containsKey(CraftuxUiHost.ACTION_JOB_JOIN));
    boolean[] invoked = {false};
    UUID audience = UUID.randomUUID();
    bus.register(CraftuxUiHost.ACTION_JOB_JOIN, (a, c) -> {
      assertEquals(audience, a);
      invoked[0] = true;
    });
    bus.proxies().get(CraftuxUiHost.ACTION_JOB_JOIN)
        .invoke(audience, new InventoryClick(audience, 10, InventoryClickKind.PICKUP));
    assertTrue(invoked[0], "proxy must dispatch to registered handler");
  }

  @Test
  void craftuxInventoryViewAcceptsJobJoinButtons() {
    InventoryView view = InventoryView.builder("job_browse", 6)
        .title("Browse Jobs")
        .decorative(0, CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE))
        .slot(10, Slot.button(
            "job_0",
            CraftuxItems.of(Material.BOOK, "Miner", List.of("Click to join!")),
            CraftuxUiHost.ACTION_JOB_JOIN,
            SlotPixelIntent.UNVALIDATED))
        .build();

    assertEquals("job_browse", view.menuId());
    assertEquals("Browse Jobs", view.title());
    Slot jobSlot = view.slots().get(10);
    assertEquals(SlotRole.BUTTON, jobSlot.role());
    assertEquals(CraftuxUiHost.ACTION_JOB_JOIN, jobSlot.actionId());
    assertEquals("minecraft:book", jobSlot.item().material());
  }

  @Test
  void craftuxUiHostDeclaresAllGuiActionIds() {
    assertEquals("modularjobs.jobs.join", CraftuxUiHost.ACTION_JOB_JOIN);
    assertEquals("modularjobs.upgrades.node", CraftuxUiHost.ACTION_UPGRADE_NODE);
    assertEquals("modularjobs.editor.canvas", CraftuxUiHost.ACTION_EDITOR_CANVAS);
  }

  @Test
  void petIntegrationRemovedFromShippedSources() throws Exception {
    Path main = Path.of("src/main/java");
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(main)) {
      files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
        try {
          String text = Files.readString(path, StandardCharsets.UTF_8);
          // Comment-only "No JobPets" notes are fine; ban real integration surfaces
          if (text.contains("JobPetsHook")
              || text.contains("PetUpgradeService")
              || text.contains("PetSelectionGui")
              || text.contains("syncPetTypeToJobPets")
              || text.contains("job_pet_selections")
              || text.contains("ACTION_PET_SELECT")
              || text.contains("modularjobs.pets.select")
              || text.contains("jobpets.pet")
              || text.contains("aincraft-mining.pet.")) {
            offenders.add(path.toString());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
    assertTrue(offenders.isEmpty(), "pet integration remnants: " + offenders);

    assertFalse(Files.exists(Path.of("src/main/java/dev/mintychochip/gui/PetSelectionGui.java")));
    assertFalse(Files.exists(Path.of("src/main/java/dev/mintychochip/service/PetUpgradeService.java")));
    assertFalse(Files.exists(Path.of("src/main/java/dev/mintychochip/hooks/JobPetsHook.java")));
    assertFalse(Files.exists(Path.of("src/main/java/dev/mintychochip/commands/UpgradeCommand.java")));

    String sql = Files.readString(
        Path.of("src/main/resources/sql/mysql.sql"), StandardCharsets.UTF_8);
    assertFalse(sql.contains("job_pet_selections"), "DDL must not create job_pet_selections");

    String pluginYml = Files.readString(
        Path.of("src/main/resources/plugin.yml"), StandardCharsets.UTF_8);
    assertFalse(pluginYml.contains("JobPets"), "plugin.yml must not soft-depend JobPets");
    assertFalse(pluginYml.contains("specialization.bypass"),
        "plugin.yml must not declare pet specialization permissions");

    String jobsYml = Files.readString(
        Path.of("src/main/resources/jobs.yml"), StandardCharsets.UTF_8);
    assertFalse(jobsYml.contains("pet-perks"), "jobs.yml must not define pet-perks");

    String jobApi = Files.readString(
        Path.of("../api/src/main/java/dev/mintychochip/Job.java"), StandardCharsets.UTF_8);
    assertFalse(jobApi.contains("petPerks"), "Job API must not expose petPerks");
    assertFalse(jobApi.contains("petRevokedPerks"), "Job API must not expose petRevokedPerks");
  }

  @Test
  void residualSurfacesUseCraftuxNotLegacyStacks() throws Exception {
    // Text scoreboard
    String scoreboard = Files.readString(
        Path.of("src/main/java/dev/mintychochip/commands/TextScoreboard.java"), StandardCharsets.UTF_8);
    assertTrue(scoreboard.contains("CraftuxSurfaces"), "TextScoreboard must use CraftuxSurfaces");
    assertFalse(scoreboard.contains("org.bukkit.scoreboard.ScoreboardManager"),
        "TextScoreboard must not open raw Bukkit scoreboards");

    // Stats inventory GUI
    String statsGui = Files.readString(
        Path.of("src/main/java/dev/mintychochip/gui/StatsGui.java"), StandardCharsets.UTF_8);
    assertTrue(statsGui.contains("InventoryRuntime"), "StatsGui must use craftux inventory");
    assertTrue(statsGui.contains("ACTION_STATS_PREV") || statsGui.contains("ACTION_STATS_NEXT"),
        "StatsGui must declare prev/next craftux actions");

    // Experience boss bar
    String xpBar = Files.readString(
        Path.of("src/main/java/dev/mintychochip/payable/ExperienceBarControllerImpl.java"),
        StandardCharsets.UTF_8);
    assertTrue(xpBar.contains("CraftuxSurfaces"), "XP bar must mount via CraftuxSurfaces");
    assertTrue(xpBar.contains("showBossBar"), "XP bar must call showBossBar");

    // Top command wires surfaces
    String top = Files.readString(
        Path.of("src/main/java/dev/mintychochip/commands/TopCommand.java"), StandardCharsets.UTF_8);
    assertTrue(top.contains("CraftuxSurfaces"), "TopCommand must take CraftuxSurfaces");
    assertTrue(top.contains("TextScoreboard.create"), "TopCommand must create TextScoreboard");
    assertTrue(top.contains("surfaces"), "TopCommand must pass surfaces into scoreboard create");

    // StatsDialog removed from shipped path
    assertFalse(Files.exists(Path.of("src/main/java/dev/mintychochip/commands/StatsDialog.java")),
        "StatsDialog Paper dialog path must be removed");
  }

  @Test
  void infoCommandUsesCraftuxNotPaperDialog() throws Exception {
    String info = Files.readString(
        Path.of("src/main/java/dev/mintychochip/commands/InfoCommand.java"), StandardCharsets.UTF_8);
    assertFalse(info.contains("io.papermc.paper.dialog"),
        "InfoCommand must not import Paper dialog packages");
    assertFalse(info.contains("showDialog"), "InfoCommand must not call showDialog");
    assertFalse(info.contains("Dialog.create"), "InfoCommand must not create Paper Dialogs");
    assertTrue(info.contains("JobInfoGui"), "InfoCommand must open JobInfoGui");
    assertTrue(info.contains("jobInfoGui.open") || info.contains("jobInfoGui.open("),
        "InfoCommand must call JobInfoGui.open on GUI path");

    String jobInfo = Files.readString(
        Path.of("src/main/java/dev/mintychochip/gui/JobInfoGui.java"), StandardCharsets.UTF_8);
    assertTrue(jobInfo.contains("InventoryRuntime"), "JobInfoGui must use craftux InventoryRuntime");
    assertTrue(jobInfo.contains("ACTION_INFO_PREV") || jobInfo.contains("ACTION_INFO_NEXT"),
        "JobInfoGui must declare craftux info navigation actions");

    assertFalse(Files.exists(Path.of("src/main/java/dev/mintychochip/commands/DialogNavigationListener.java")),
        "DialogNavigationListener must be removed (info nav is craftux actions)");

    // No Paper dialog usage remains on the main shipped path
    Path main = Path.of("src/main/java");
    List<String> dialogOffenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(main)) {
      files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
        try {
          String text = Files.readString(path, StandardCharsets.UTF_8);
          if (text.contains("io.papermc.paper.dialog") || text.contains("showDialog")) {
            dialogOffenders.add(path.toString());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
    assertTrue(dialogOffenders.isEmpty(),
        "Paper dialog remnants remain in: " + dialogOffenders);
  }

  @Test
  void statsGuiBuildsPaginatedCraftuxView() {
    InventoryView view = InventoryView.builder("job_stats", 6)
        .title("Stats: Player (1/1)")
        .decorative(4, CraftuxItems.of(Material.BOOK, "Job Statistics", List.of("Jobs: 0")))
        .slot(45, Slot.navigation(
            "stats_prev",
            CraftuxItems.of(Material.ARROW, "Previous", List.of()),
            CraftuxUiHost.ACTION_STATS_PREV,
            SlotPixelIntent.UNVALIDATED))
        .slot(53, Slot.navigation(
            "stats_next",
            CraftuxItems.of(Material.ARROW, "Next", List.of()),
            CraftuxUiHost.ACTION_STATS_NEXT,
            SlotPixelIntent.UNVALIDATED))
        .build();
    assertEquals("job_stats", view.menuId());
    assertEquals(CraftuxUiHost.ACTION_STATS_PREV, view.slots().get(45).actionId());
    assertEquals(CraftuxUiHost.ACTION_STATS_NEXT, view.slots().get(53).actionId());
  }
}
