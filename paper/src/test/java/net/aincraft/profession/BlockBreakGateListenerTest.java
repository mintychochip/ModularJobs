package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.aincraft.service.ProfessionService;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

class BlockBreakGateListenerTest {

  private static final String BYPASS = "modularjobs.bypassblockbreak";

  private StubProfessionService professions;

  private record StubProfessionService(
      java.util.Map<String, OptionalInt> levels) implements ProfessionService {

    @Override
    public List<net.aincraft.profession.ProfessionDefinition> tracks() {
      return List.of();
    }

    @Override
    public Optional<net.aincraft.profession.ProfessionDefinition> resolve(String idOrAlias) {
      return net.aincraft.profession.ProfessionCatalog.resolve(idOrAlias);
    }

    @Override
    public OptionalInt level(UUID playerId, String professionIdOrAlias) {
      return levels.getOrDefault(professionIdOrAlias, OptionalInt.empty());
    }

    @Override
    public Optional<java.math.BigDecimal> experience(UUID playerId, String professionIdOrAlias) {
      return Optional.empty();
    }

    @Override
    public boolean ensureTrack(UUID playerId, String professionIdOrAlias) {
      return true;
    }
  }

  private BlockBreakGateListener listener;
  private Player player;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    professions = new StubProfessionService(new java.util.HashMap<>());
    BlockBreakGateStore store = new BlockBreakGateStore(List.of(
        new BlockBreakGate("diamond_ore", "mining", 30)));
    listener = new BlockBreakGateListener(store, professions);
    player = MockBukkitSupport.mockServer().addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void belowRequiredLevelCancelsBreak() {
    professions.levels().put("mining", OptionalInt.of(29));
    assertTrue(breakEvent().isCancelled());
  }

  @Test
  void atRequiredLevelAllowsBreak() {
    professions.levels().put("mining", OptionalInt.of(30));
    assertFalse(breakEvent().isCancelled());
  }

  @Test
  void aboveRequiredLevelAllowsBreak() {
    professions.levels().put("mining", OptionalInt.of(45));
    assertFalse(breakEvent().isCancelled());
  }

  @Test
  void unjoinedProfessionCancelsBreak() {
    // no level entry -> OptionalInt.empty -> treated as level 0
    assertTrue(breakEvent().isCancelled());
  }

  @Test
  void ungatedMaterialAllowsBreak() {
    Block block = mockBlock(Material.STONE);
    BlockBreakEvent event = new BlockBreakEvent(block, player);
    listener.onBlockBreak(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void bypassPermissionAllowsBreak() {
    professions.levels().put("mining", OptionalInt.of(1));
    ServerMock server = MockBukkitSupport.mockServer();
    org.mockbukkit.mockbukkit.plugin.PluginMock plugin =
        org.mockbukkit.mockbukkit.plugin.PluginMock.builder()
            .withPluginName("MockBukkit")
            .build();
    server.getPluginManager().registerLoadedPlugin(plugin);
    player.addAttachment(plugin).setPermission(BYPASS, true);
    assertFalse(breakEvent().isCancelled());
  }

  private BlockBreakEvent breakEvent() {
    Block block = mockBlock(Material.DIAMOND_ORE);
    BlockBreakEvent event = new BlockBreakEvent(block, player);
    listener.onBlockBreak(event);
    return event;
  }

  private Block mockBlock(Material material) {
    var server = org.bukkit.Bukkit.getServer();
    org.bukkit.World world = server.getWorlds().isEmpty()
        ? server.createWorld(new org.bukkit.WorldCreator("world"))
        : server.getWorlds().getFirst();
    Block block = world.getBlockAt(0, 64, 0);
    block.setType(material);
    return block;
  }
}
