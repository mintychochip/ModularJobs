package net.aincraft.payable;

import com.google.inject.Inject;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableHandler;

final class EconomyPayableHandlerImpl implements PayableHandler {

  private static final Logger LOGGER = Logger.getLogger(EconomyPayableHandlerImpl.class.getName());

  @Nullable
  private final EconomyProvider provider;

  @Inject
  public EconomyPayableHandlerImpl(@javax.annotation.Nullable EconomyProvider provider) {
    this.provider = provider;
    LOGGER.info("EconomyPayableHandlerImpl initialized with provider: " + (provider != null ? provider.getClass().getName() : "null"));
  }

  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    if (provider == null) {
      LOGGER.warning("Cannot pay - no economy provider available");
      return;
    }
    LOGGER.info("Paying " + context.payable().amount().value() + " to " + context.player().getName());
    boolean success = provider.deposit(context.player(), context.payable().amount());
    LOGGER.info("Payment result: " + success);
  }
}
