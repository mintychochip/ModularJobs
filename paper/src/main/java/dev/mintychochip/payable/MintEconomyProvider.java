package dev.mintychochip.payable;

import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.PayableAmount;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Mint ledger bridge.
 *
 * <p>This class deliberately contains no compile-time Mint references. Mint is resolved lazily so
 * its asynchronous service registration can complete after ModularJobs enables.
 */
public final class MintEconomyProvider implements EconomyProvider {

  private static final Logger LOGGER = Logger.getLogger(MintEconomyProvider.class.getName());
  private static final String MINT_PLUGIN = "Mint";
  private static final String MINT_SERVICE = "dev.jlo.mint.api.service.Mint";
  private static final String LEDGER_SERVICE = "dev.jlo.mint.api.service.LedgerService";
  private static final String ISSUE_REQUEST = "dev.jlo.mint.api.ledger.IssueRequest";
  private static final String ACCOUNT_ID = "dev.jlo.mint.api.id.AccountId";
  private static final String ACTOR_ID = "dev.jlo.mint.api.id.ActorId";
  private static final String CLIENT_ID = "dev.jlo.mint.api.id.ClientId";
  private static final String CURRENCY_ID = "dev.jlo.mint.api.id.CurrencyId";
  private static final String IDEMPOTENCY_KEY = "dev.jlo.mint.api.id.IdempotencyKey";
  private static final String NAMESPACE_ID = "dev.jlo.mint.api.id.NamespaceId";
  private static final String MONEY = "dev.jlo.mint.api.money.Money";
  private static final String COMMITTED = "dev.jlo.mint.api.result.Committed";
  private static final String REJECTED = "dev.jlo.mint.api.result.Rejected";
  private static final String ACTOR_NAMESPACE = "modularjobs:reward";
  private static final String CURRENCY_NAMESPACE = "modularjobs:coin";
  private static final String CLIENT_NAMESPACE = "modularjobs:jobs";
  private static final String REASON = "modularjobs job reward";
  private static final long TIMEOUT_MILLIS = 5_000L;

  private final AtomicBoolean reflectionFailureLogged = new AtomicBoolean();
  private final AtomicReference<MintReflection> reflection = new AtomicReference<>();
  private final AtomicBoolean reflectionUnavailable = new AtomicBoolean();

  /** Visible for selection tests; callers go through {@link EconomyProviderFactory}. */
  public MintEconomyProvider() {}

  /**
   * Checks only whether the optional service API can be loaded. Service readiness is checked lazily
   * by {@link #deposit} to avoid racing Mint's asynchronous Bukkit registration.
   */
  static boolean isApiAvailable() {
    try {
      loadClass(MINT_SERVICE);
      return true;
    } catch (ClassNotFoundException | LinkageError ignored) {
      return false;
    }
  }

  @Override
  public boolean isCurrencySupported() {
    return true;
  }

  @Override
  public boolean deposit(UUID playerId, PayableAmount payableAmount) {
    BigDecimal amount = payableAmount == null ? null : payableAmount.value();
    if (amount == null || amount.signum() <= 0) {
      return false;
    }

    MintReflection bridge = reflection();
    if (bridge == null) {
      return false;
    }

    try {
      Object mint = bridge.resolveMint();
      if (mint == null) {
        return false;
      }
      return bridge.issue(mint, playerId, amount);
    } catch (TimeoutException e) {
      LOGGER.severe(
          "Mint issue timed out for "
              + playerId
              + " after "
              + TIMEOUT_MILLIS
              + "ms — outcome unknown, do not retry (at-most-once)");
      return false;
    } catch (LinkageError e) {
      LOGGER.log(Level.SEVERE, "Mint issue failed for player " + playerId, e);
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.SEVERE, "Mint issue failed for player " + playerId, e);
      return false;
    } catch (ReflectiveOperationException | java.util.concurrent.ExecutionException e) {
      LOGGER.log(Level.SEVERE, "Mint issue failed for player " + playerId, e);
      return false;
    }
  }

  private @Nullable MintReflection reflection() {
    MintReflection current = reflection.get();
    if (current != null) {
      return current;
    }
    if (reflectionUnavailable.get()) {
      return null;
    }
    try {
      current = MintReflection.load();
      reflection.set(current);
      return current;
    } catch (ReflectiveOperationException | LinkageError e) {
      reflectionUnavailable.set(true);
      if (reflectionFailureLogged.compareAndSet(false, true)) {
        LOGGER.log(Level.WARNING, "Mint API is unavailable; Mint economy rewards are disabled", e);
      }
      return null;
    }
  }

  private static Class<?> loadClass(String name) throws ClassNotFoundException {
    ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    if (contextLoader != null) {
      try {
        return Class.forName(name, false, contextLoader);
      } catch (ClassNotFoundException ignored) {
        // Fall back to the default loader chain below.
      }
    }
    return Class.forName(name);
  }

  private static final class MintReflection {

    private final Class<?> mintServiceClass;
    private final Method mintState;
    private final Method mintClient;
    private final Method clientLedger;
    private final Method ledgerIssue;
    private final Method namespaceParse;
    private final Method actorOf;
    private final Method currencyOf;
    private final Method clientOf;
    private final Method accountPlayer;
    private final Constructor<?> idempotencyKey;
    private final Constructor<?> money;
    private final Constructor<?> issueRequest;
    private final Class<?> committedClass;
    private final Class<?> rejectedClass;

    private MintReflection(
        Class<?> mintServiceClass,
        Method mintState,
        Method mintClient,
        Method clientLedger,
        Method ledgerIssue,
        Method namespaceParse,
        Method actorOf,
        Method currencyOf,
        Method clientOf,
        Method accountPlayer,
        Constructor<?> idempotencyKey,
        Constructor<?> money,
        Constructor<?> issueRequest,
        Class<?> committedClass,
        Class<?> rejectedClass) {
      this.mintServiceClass = mintServiceClass;
      this.mintState = mintState;
      this.mintClient = mintClient;
      this.clientLedger = clientLedger;
      this.ledgerIssue = ledgerIssue;
      this.namespaceParse = namespaceParse;
      this.actorOf = actorOf;
      this.currencyOf = currencyOf;
      this.clientOf = clientOf;
      this.accountPlayer = accountPlayer;
      this.idempotencyKey = idempotencyKey;
      this.money = money;
      this.issueRequest = issueRequest;
      this.committedClass = committedClass;
      this.rejectedClass = rejectedClass;
    }

    private static MintReflection load() throws ReflectiveOperationException {
      Class<?> mintService = loadClass(MINT_SERVICE);
      Class<?> ledgerService = loadClass(LEDGER_SERVICE);
      Class<?> issueRequestClass = loadClass(ISSUE_REQUEST);
      Class<?> accountId = loadClass(ACCOUNT_ID);
      Class<?> actorId = loadClass(ACTOR_ID);
      Class<?> clientId = loadClass(CLIENT_ID);
      Class<?> currencyId = loadClass(CURRENCY_ID);
      Class<?> idempotencyKeyClass = loadClass(IDEMPOTENCY_KEY);
      Class<?> namespaceId = loadClass(NAMESPACE_ID);
      Class<?> moneyClass = loadClass(MONEY);
      Class<?> committedClass = loadClass(COMMITTED);
      Class<?> rejectedClass = loadClass(REJECTED);

      Method mintStateMethod = mintService.getMethod("state");
      Method mintClientMethod = mintService.getMethod("client", clientId);
      Method clientLedgerMethod = mintClientMethod.getReturnType().getMethod("ledger");
      Method ledgerIssueMethod = ledgerService.getMethod("issue", issueRequestClass);
      Method namespaceParseMethod = namespaceId.getMethod("parse", String.class);
      Method actorOfMethod = actorId.getMethod("of", namespaceId);
      Method currencyOfMethod = currencyId.getMethod("of", namespaceId);
      Method clientOfMethod = clientId.getMethod("of", namespaceId);
      Method accountPlayerMethod = accountId.getMethod("player", UUID.class);

      return new MintReflection(
          mintService,
          mintStateMethod,
          mintClientMethod,
          clientLedgerMethod,
          ledgerIssueMethod,
          namespaceParseMethod,
          actorOfMethod,
          currencyOfMethod,
          clientOfMethod,
          accountPlayerMethod,
          idempotencyKeyClass.getConstructor(String.class),
          moneyClass.getConstructor(currencyId, BigDecimal.class),
          issueRequestClass.getConstructor(
              idempotencyKeyClass, actorId, accountId, moneyClass, String.class, Map.class),
          committedClass,
          rejectedClass);
    }

    private @Nullable Object resolveMint() throws ReflectiveOperationException {
      if (Bukkit.getPluginManager() == null
          || !Bukkit.getPluginManager().isPluginEnabled(MINT_PLUGIN)
          || Bukkit.getServicesManager() == null) {
        return null;
      }
      @SuppressWarnings({"rawtypes", "unchecked"})
      RegisteredServiceProvider<?> registration =
          Bukkit.getServicesManager().getRegistration((Class) mintServiceClass);
      if (registration == null || registration.getProvider() == null) {
        return null;
      }
      Object mint = registration.getProvider();
      Object state = mintState.invoke(mint);
      if (!isReady(state)) {
        return null;
      }
      return mint;
    }

    private boolean issue(Object mint, UUID playerId, BigDecimal amount)
        throws ReflectiveOperationException,
            InterruptedException,
            java.util.concurrent.ExecutionException,
            TimeoutException {
      Object namespace = namespaceParse.invoke(null, ACTOR_NAMESPACE);
      Object actor = actorOf.invoke(null, namespace);
      Object currency = currencyOf.invoke(null, namespaceParse.invoke(null, CURRENCY_NAMESPACE));
      Object client = clientOf.invoke(null, namespaceParse.invoke(null, CLIENT_NAMESPACE));
      Object account = accountPlayer.invoke(null, playerId);
      Object key = idempotencyKey.newInstance("modularjobs:" + UUID.randomUUID());
      Object value = money.newInstance(currency, amount);
      Object request = issueRequest.newInstance(key, actor, account, value, REASON, Map.of());
      Object clientService = mintClient.invoke(mint, client);
      Object ledger = clientLedger.invoke(clientService);
      Object result = ledgerIssue.invoke(ledger, request);
      if (!(result instanceof CompletionStage<?> stage)) {
        throw new IllegalStateException("Mint LedgerService.issue did not return CompletionStage");
      }
      Object outcome = stage.toCompletableFuture().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      if (committedClass.isInstance(outcome)) {
        LOGGER.fine("Mint issued " + amount + " to " + playerId);
        return true;
      }
      if (rejectedClass.isInstance(outcome)) {
        LOGGER.warning("Mint issue rejected for " + playerId + ": " + rejectionMessage(outcome));
      }
      return false;
    }

    private static boolean isReady(Object state) {
      return state instanceof Enum<?> value
          ? "READY".equals(value.name())
          : "READY".equals(String.valueOf(state));
    }

    private static String rejectionMessage(Object rejected) {
      try {
        Object rejection = rejected.getClass().getMethod("rejection").invoke(rejected);
        return String.valueOf(rejection.getClass().getMethod("message").invoke(rejection));
      } catch (ReflectiveOperationException ignored) {
        return "unknown reason";
      }
    }
  }
}
