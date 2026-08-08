package net.aincraft.payable;

import dev.jlo.mint.api.id.AccountId;
import dev.jlo.mint.api.id.ActorId;
import dev.jlo.mint.api.id.ClientId;
import dev.jlo.mint.api.id.CurrencyId;
import dev.jlo.mint.api.id.IdempotencyKey;
import dev.jlo.mint.api.id.NamespaceId;
import dev.jlo.mint.api.ledger.IssueRequest;
import dev.jlo.mint.api.ledger.TransactionReceipt;
import dev.jlo.mint.api.lifecycle.MintState;
import dev.jlo.mint.api.money.Money;
import dev.jlo.mint.api.result.Committed;
import dev.jlo.mint.api.result.OperationOutcome;
import dev.jlo.mint.api.result.Rejected;
import dev.jlo.mint.api.service.LedgerService;
import dev.jlo.mint.api.service.Mint;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableAmount;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Mint (aincraft-org) ledger bridge. Deposits are {@code LedgerService.issue} credits to the
 * player's account; an {@code IssueRequest} auto-creates the balance row (no account pre-ensuring,
 * and currency mutations are deferred in the foundation build).
 *
 * <p>The {@code Mint} service is resolved lazily per deposit because Mint registers it
 * asynchronously at startup (next-tick main-thread callback); eagerly capturing it at wiring time
 * would race the ledger's boot.
 *
 * <p><strong>At-most-once semantics:</strong> each {@link #deposit} invocation issues a fresh
 * idempotency key, exactly like the Vault bridge it replaces. The upstream payment pipeline does
 * not (yet) carry a durable payout ID, so a crash between a committed issue and the caller seeing
 * the result can double-credit — the Mint idempotency ledger cannot dedupe what the caller never
 * identified. Do not build retry logic on top of this provider.
 */
public final class MintEconomyProvider implements EconomyProvider {

  private static final Logger LOGGER = Logger.getLogger(MintEconomyProvider.class.getName());
  private static final ActorId ACTOR = ActorId.of(NamespaceId.parse("modularjobs:reward"));
  private static final CurrencyId CURRENCY = CurrencyId.of(NamespaceId.parse("modularjobs:coin"));
  private static final ClientId CLIENT = ClientId.of(NamespaceId.parse("modularjobs:jobs"));
  private static final String REASON = "modularjobs job reward";
  private static final long TIMEOUT_MILLIS = 5_000L;

  /** Visible for the selection unit test; callers go through {@link EconomyProviderFactory}. */
  public MintEconomyProvider() {}

  @Override
  public boolean isCurrencySupported() {
    return true;
  }

  @Override
  public boolean deposit(UUID playerId, PayableAmount payableAmount) {
    BigDecimal amount = payableAmount.value();
    if (amount == null || amount.signum() <= 0) {
      return false;
    }
    Mint mint = resolveMint();
    if (mint == null) {
      return false;
    }
    LedgerService ledger = mint.client(CLIENT).ledger();
    IssueRequest request = new IssueRequest(
        new IdempotencyKey("modularjobs:" + UUID.randomUUID()),
        ACTOR,
        AccountId.player(playerId),
        new Money(CURRENCY, amount),
        REASON,
        Map.of());
    try {
      CompletionStage<OperationOutcome<TransactionReceipt>> future = ledger.issue(request);
      OperationOutcome<TransactionReceipt> outcome = future.toCompletableFuture()
          .get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      if (outcome instanceof Committed<TransactionReceipt> committed) {
        LOGGER.fine("Issued " + amount + " " + CURRENCY + " to " + playerId
            + " (tx " + committed.value().transactionId() + ")");
        return true;
      }
      if (outcome instanceof Rejected<TransactionReceipt> rejected) {
        LOGGER.warning("Mint issue rejected for " + playerId + ": "
            + rejected.rejection().message());
        return false;
      }
      return false;
    } catch (TimeoutException e) {
      LOGGER.severe("Mint issue timed out for " + playerId + " after " + TIMEOUT_MILLIS
          + "ms — outcome unknown, do not retry (at-most-once)");
      return false;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Mint issue failed for player " + playerId, e);
      return false;
    }
  }

  /**
   * Resolves the {@code Mint} Bukkit service when registered and READY; else null (logs once per
   * miss at warning for the first miss, then quiet).
   */
  private @org.jetbrains.annotations.Nullable Mint resolveMint() {
    if (Bukkit.getPluginManager() == null
        || !Bukkit.getPluginManager().isPluginEnabled("Mint")) {
      return null;
    }
    RegisteredServiceProvider<Mint> registration = Bukkit.getServicesManager().getRegistration(Mint.class);
    if (registration == null || registration.getProvider() == null) {
      return null;
    }
    Mint mint = registration.getProvider();
    if (mint.state() != MintState.READY) {
      return null;
    }
    return mint;
  }
}
