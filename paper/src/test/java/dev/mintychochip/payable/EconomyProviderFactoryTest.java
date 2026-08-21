package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import dev.mintychochip.container.Currency;
import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableHandler.PayableContext;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.test.MockBukkitSupport;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Drives shipped {@link EconomyProviderFactory} and {@link PayableWiring#economyHandlerFor}
 * selection and payment behavior without a live Mint server.
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
  void createOrFailUsesBlackholeByDefault() {
    EconomyProvider provider = EconomyProviderFactory.createOrFail(plugin);

    assertInstanceOf(BlackholeEconomyProvider.class, provider);
    assertTrue(provider.isCurrencySupported());
  }

  @Test
  void createOrFailThrowsWhenExplicitFailPolicyHasNoProvider() {
    plugin.getConfig().set("economy.missing-provider", "fail");

    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> EconomyProviderFactory.createOrFail(plugin));

    assertTrue(ex.getMessage().contains("economy.missing-provider"));
  }

  @Test
  void legacyRequiredTrueStillFailsWithoutProvider() {
    plugin.getConfig().set("economy.missing-provider", null);
    plugin.getConfig().set("economy.required", true);

    assertThrows(
        IllegalStateException.class,
        () -> EconomyProviderFactory.createOrFail(plugin));
  }

  @Test
  void explicitBlackholePolicyOverridesLegacyRequiredFlag() {
    plugin.getConfig().set("economy.required", true);
    plugin.getConfig().set("economy.missing-provider", "BLACKHOLE");

    assertInstanceOf(
        BlackholeEconomyProvider.class,
        EconomyProviderFactory.createOrFail(plugin));
  }

  @Test
  void unknownMissingProviderPolicyFailsConfiguration() {
    plugin.getConfig().set("economy.missing-provider", "unknown");

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> EconomyProviderFactory.createOrFail(plugin));

    assertTrue(ex.getMessage().contains("economy.missing-provider"));
  }

  @Test
  void blackholeAcceptsPositiveAndRejectsNonPositiveAmounts() {
    EconomyProvider provider = new BlackholeEconomyProvider(plugin);

    assertTrue(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ONE)));
    assertFalse(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ZERO)));
    assertFalse(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ONE.negate())));
  }

  @Test
  void economyHandlerDelegatesToProvider() {
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
    assertDoesNotThrow(() -> handler.pay(contextFor(handler, player)));
    assertTrue(deposited[0], "shipped handler must call EconomyProvider.deposit");
  }

  @Test
  void requiredDefaultsFalse() {
    Plugin fresh = MockBukkit.createMockPlugin("JobsFresh");

    assertFalse(EconomyProviderFactory.isRequired(fresh));
  }

  private static PayableContext contextFor(PayableHandler handler, OfflinePlayer player) {
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
    return new PayableContext(player.getUniqueId(), payable, null);
  }
}
