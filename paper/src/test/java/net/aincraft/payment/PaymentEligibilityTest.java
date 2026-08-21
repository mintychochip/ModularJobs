package net.aincraft.payment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Set;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Drives shipped {@link PaymentEligibility} with real {@link PaymentSettings} values (no
 * {@code && false} stubs).
 */
class PaymentEligibilityTest {

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void blocksCreativeWhenPayInCreativeFalse() {
    PaymentEligibility eligibility = new PaymentEligibility(
        new PaymentSettings(false, true, Set.of(), 0.5, 25.0));
    PlayerMock player = MockBukkitSupport.mockServer().addPlayer();
    player.setGameMode(GameMode.CREATIVE);
    assertTrue(eligibility.blocksPay(player));

    player.setGameMode(GameMode.SURVIVAL);
    assertFalse(eligibility.blocksPay(player));
  }

  @Test
  void allowsCreativeWhenPayInCreativeTrue() {
    PaymentEligibility eligibility = new PaymentEligibility(
        new PaymentSettings(true, true, Set.of(), 0.5, 25.0));
    PlayerMock player = MockBukkitSupport.mockServer().addPlayer();
    player.setGameMode(GameMode.CREATIVE);
    assertFalse(eligibility.blocksPay(player));
  }

  @Test
  void blocksRidingWhenPayWhileRidingFalse() {
    PaymentEligibility eligibility = new PaymentEligibility(
        new PaymentSettings(true, false, Set.of(), 0.5, 25.0));
    PlayerMock base = MockBukkitSupport.mockServer().addPlayer();
    World world = base.getWorld();
    Player riding = playerStub(world, GameMode.SURVIVAL, true);
    assertTrue(eligibility.blocksPay(riding));

    PaymentEligibility allowRiding = new PaymentEligibility(
        new PaymentSettings(true, true, Set.of(), 0.5, 25.0));
    assertFalse(allowRiding.blocksPay(riding));
  }

  @Test
  void blocksDisabledWorldCaseInsensitive() {
    PaymentEligibility eligibility = new PaymentEligibility(
        new PaymentSettings(true, true, Set.of("world_nether"), 0.5, 25.0));
    PlayerMock player = MockBukkitSupport.mockServer().addPlayer();
    String worldName = player.getWorld().getName();
    PaymentEligibility matching = new PaymentEligibility(
        new PaymentSettings(true, true, Set.of(worldName.toLowerCase()), 0.5, 25.0));
    assertTrue(matching.blocksPay(player));
    assertFalse(eligibility.blocksPay(player)); // world_nether != default world
  }

  @Test
  void blocksAdventureModeAlways() {
    PaymentEligibility eligibility = new PaymentEligibility(PaymentSettings.defaults());
    PlayerMock player = MockBukkitSupport.mockServer().addPlayer();
    player.setGameMode(GameMode.ADVENTURE);
    assertTrue(eligibility.blocksPay(player));
  }

  private static Player playerStub(World world, GameMode mode, boolean insideVehicle) {
    return (Player) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(),
        new Class<?>[] {Player.class},
        (proxy, method, args) -> {
          return switch (method.getName()) {
            case "getWorld" -> world;
            case "getGameMode" -> mode;
            case "isInsideVehicle" -> insideVehicle;
            case "hasMetadata" -> false;
            case "equals" -> Objects.equals(proxy, args[0]);
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "PlayerStub";
            default -> {
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) {
                yield false;
              }
              if (rt == int.class) {
                yield 0;
              }
              if (rt == long.class) {
                yield 0L;
              }
              if (rt == double.class) {
                yield 0.0d;
              }
              if (rt == float.class) {
                yield 0.0f;
              }
              yield null;
            }
          };
        });
  }
}
