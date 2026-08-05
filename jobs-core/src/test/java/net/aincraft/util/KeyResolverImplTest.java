package net.aincraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.aincraft.container.Context.MaterialContext;
import net.aincraft.test.MockBukkitSupport;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link KeyResolverImpl} strategy registration and resolve dispatch.
 * Material keys require MockBukkit server lifecycle on Paper 26.2.
 */
class KeyResolverImplTest {

  private KeyResolverImpl resolver;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    resolver = new KeyResolverImpl();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void resolveWithoutStrategyReturnsNull() {
    MaterialContext ctx = new MaterialContext(Material.STONE);
    assertNull(resolver.resolve(ctx));
  }

  @Test
  void resolveUsesRegisteredStrategy() {
    resolver.addStrategy(MaterialContext.class, context -> context.material().getKey());

    Key stone = resolver.resolve(new MaterialContext(Material.STONE));
    Key dirt = resolver.resolve(new MaterialContext(Material.DIRT));

    assertEquals(Material.STONE.getKey(), stone);
    assertEquals(Material.DIRT.getKey(), dirt);
    assertEquals(Key.key("minecraft", "stone"), stone);
  }

  @Test
  void strategyReplacementOverridesPrior() {
    resolver.addStrategy(MaterialContext.class, context -> Key.key("test", "first"));
    resolver.addStrategy(MaterialContext.class, context -> Key.key("test", "second"));

    Key result = resolver.resolve(new MaterialContext(Material.STONE));
    assertEquals(Key.key("test", "second"), result);
  }
}
