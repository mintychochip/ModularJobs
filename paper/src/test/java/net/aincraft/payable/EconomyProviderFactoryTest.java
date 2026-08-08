package net.aincraft.payable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import net.aincraft.container.Currency;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableHandler.PayableContext;
import net.aincraft.container.PayableType;
import net.aincraft.test.MockBukkitSupport;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

/**
 * Drives shipped {@link EconomyProviderFactory} and {@link PayableWiring#economyHandlerFor}
 * selection / null hard-fail behavior without a live Mint server.
 */
class EconomyProviderFactoryTest {

  private Plugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void tryCreateReturnsNullWithoutMint() {
    assertNull(EconomyProviderFactory.tryCreate(plugin));
  }

  @Test
  void createOrFailThrowsWhenRequiredAndNoProvider() {
    plugin.getConfig().set("economy.required", true);
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> EconomyProviderFactory.createOrFail(plugin));
    assertTrue(ex.getMessage().toLowerCase().contains("economy")
        || ex.getMessage().toLowerCase().contains("mint"));
  }

  @Test
  void createOrFailReturnsNullWhenNotRequiredAndNoProvider() {
    plugin.getConfig().set("economy.required", false);
    assertNull(EconomyProviderFactory.createOrFail(plugin));
  }

  @Test
  void economyHandlerThrowsWhenProviderNullNotSilentNoOp() {
    PayableHandler handler = PayableWiring.economyHandlerFor(null);
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    PayableType type = new PayableType() {
      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return Key.key("modularjobs", "economy");
      }

      @Override
      public net.kyori.adventure.text.Component render(PayableAmount amount, int places) {
        return net.kyori.adventure.text.Component.empty();
      }
    };
    Payable payable = new Payable(
        type, PayableAmount.create(BigDecimal.ONE, Currency.USD));
    PayableContext ctx = new PayableContext(player.getUniqueId(), payable, null);
    assertThrows(IllegalStateException.class, () -> handler.pay(ctx));
  }

  @Test
  void economyHandlerDepositsWhenProviderPresent() {
    boolean[] deposited = {false};
    EconomyProvider provider = new EconomyProvider() {
      @Override
      public boolean isCurrencySupported() {
        return false;
      }

      @Override
      public boolean deposit(UUID playerId, PayableAmount payableAmount) {
        deposited[0] = true;
        return true;
      }
    };
    PayableHandler handler = PayableWiring.economyHandlerFor(provider);
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    PayableType type = new PayableType() {
      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return Key.key("modularjobs", "economy");
      }

      @Override
      public net.kyori.adventure.text.Component render(PayableAmount amount, int places) {
        return net.kyori.adventure.text.Component.empty();
      }
    };
    Payable payable = new Payable(
        type, PayableAmount.create(BigDecimal.TEN, Currency.USD));
    assertDoesNotThrow(() -> handler.pay(new PayableContext(player.getUniqueId(), payable, null)));
    assertTrue(deposited[0], "shipped handler must call EconomyProvider.deposit");
  }

  @Test
  void isRequiredDefaultsTrue() {
    // fresh mock plugin config has no economy.required key
    Plugin fresh = MockBukkit.createMockPlugin("JobsFresh");
    assertTrue(EconomyProviderFactory.isRequired(fresh));
  }
}
