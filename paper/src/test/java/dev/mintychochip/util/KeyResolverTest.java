package dev.mintychochip.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.mintychochip.container.Context.MaterialContext;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link KeyResolver} strategy registration and resolve dispatch.
 * Pure Context material keys need no MockBukkit lifecycle.
 */
class KeyResolverTest {

  private KeyResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new KeyResolver();
  }

  @Test
  void resolveWithoutStrategyReturnsNull() {
    MaterialContext ctx = new MaterialContext("minecraft:stone");
    assertNull(resolver.resolve(ctx));
  }

  @Test
  void resolveUsesRegisteredStrategy() {
    resolver.addStrategy(MaterialContext.class, context -> Key.key(context.materialKey()));

    Key stone = resolver.resolve(new MaterialContext("minecraft:stone"));
    Key dirt = resolver.resolve(new MaterialContext("minecraft:dirt"));

    assertEquals(Key.key("minecraft", "stone"), stone);
    assertEquals(Key.key("minecraft", "dirt"), dirt);
  }

  @Test
  void strategyReplacementOverridesPrior() {
    resolver.addStrategy(MaterialContext.class, context -> Key.key("test", "first"));
    resolver.addStrategy(MaterialContext.class, context -> Key.key("test", "second"));

    Key result = resolver.resolve(new MaterialContext("minecraft:stone"));
    assertEquals(Key.key("test", "second"), result);
  }
}
