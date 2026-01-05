package net.aincraft.payable;

import com.google.inject.Inject;
import javax.annotation.Nullable;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableHandler;

final class EconomyPayableHandlerImpl implements PayableHandler {

  private final EconomyProvider provider;

  @Inject
  public EconomyPayableHandlerImpl(@javax.annotation.Nullable EconomyProvider provider) {
    this.provider = provider;
  }

  @Override
  public void pay(PayableContext context) throws IllegalArgumentException {
    if (provider == null) {
      return;
    }
    provider.deposit(context.player(), context.payable().amount());
  }
}
