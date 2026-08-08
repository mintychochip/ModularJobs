package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.aincraft.service.ProfessionService;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.CodMock;
import org.mockbukkit.mockbukkit.entity.FishHookMock;
import org.mockbukkit.mockbukkit.entity.ItemMock;

class FishCatchGateListenerTest {

  private static final String BYPASS = "modularjobs.bypassfishcatch";

  private StubProfessionService professions;
  private FishCatchGateListener listener;
  private Player player;

  private record StubProfessionService(Map<String, OptionalInt> levels)
      implements ProfessionService {

    @Override
    public List<ProfessionDefinition> tracks() {
      return List.of();
    }

    @Override
    public Optional<ProfessionDefinition> resolve(String idOrAlias) {
      return ProfessionCatalog.resolve(idOrAlias);
    }

    @Override
    public OptionalInt level(UUID playerId, String professionIdOrAlias) {
      return levels.getOrDefault(professionIdOrAlias, OptionalInt.empty());
    }

    @Override
    public Optional<BigDecimal> experience(UUID playerId, String professionIdOrAlias) {
      return Optional.empty();
    }

    @Override
    public boolean ensureTrack(UUID playerId, String professionIdOrAlias) {
      return true;
    }
  }

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    professions = new StubProfessionService(new HashMap<>());
    FishCatchGateStore store = new FishCatchGateStore(
        List.of(new FishCatchGate("salmon", "fishing", 10)));
    listener = new FishCatchGateListener(store, professions);
    player = MockBukkitSupport.mockServer().addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void belowRequiredLevelCancelsCatch() {
    professions.levels().put("fishing", OptionalInt.of(9));
    PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);

    listener.onFish(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void atRequiredLevelAllowsCatch() {
    professions.levels().put("fishing", OptionalInt.of(10));
    PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);

    listener.onFish(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void aboveRequiredLevelAllowsCatch() {
    professions.levels().put("fishing", OptionalInt.of(25));
    PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);

    listener.onFish(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void unjoinedProfessionCancelsCatch() {
    PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);

    listener.onFish(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void unconfiguredFishAllowsCatch() {
    PlayerFishEvent event = fishEvent(Material.COD, PlayerFishEvent.State.CAUGHT_FISH);

    listener.onFish(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void nonCatchStateAllowsEvent() {
    PlayerFishEvent event = fishEvent((Material) null, PlayerFishEvent.State.BITE);

    listener.onFish(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void nonItemCaughtEntityAllowsEvent() {
    PlayerFishEvent event = fishEvent(
        new CodMock(MockBukkitSupport.mockServer(), UUID.randomUUID()),
        PlayerFishEvent.State.CAUGHT_FISH);

    listener.onFish(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void bypassPermissionAllowsCatch() {
    professions.levels().put("fishing", OptionalInt.of(1));
    ServerMock server = MockBukkitSupport.mockServer();
    org.mockbukkit.mockbukkit.plugin.PluginMock plugin =
        org.mockbukkit.mockbukkit.plugin.PluginMock.builder()
            .withPluginName("MockBukkit")
            .build();
    server.getPluginManager().registerLoadedPlugin(plugin);
    player.addAttachment(plugin).setPermission(BYPASS, true);

    PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);
    listener.onFish(event);

    assertFalse(event.isCancelled());
  }

  private PlayerFishEvent fishEvent(Material material, PlayerFishEvent.State state) {
    Entity caught = state == PlayerFishEvent.State.CAUGHT_FISH
        ? new ItemMock(
            MockBukkitSupport.mockServer(), UUID.randomUUID(), new ItemStack(material))
        : null;
    return fishEvent(caught, state);
  }

  private PlayerFishEvent fishEvent(Entity caught, PlayerFishEvent.State state) {
    ServerMock server = MockBukkitSupport.mockServer();
    FishHookMock hook = new FishHookMock(server, UUID.randomUUID());
    return new PlayerFishEvent(player, caught, hook, state);
  }
}
