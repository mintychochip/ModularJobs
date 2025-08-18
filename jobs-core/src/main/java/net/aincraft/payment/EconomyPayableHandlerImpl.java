package net.aincraft.payment;

import com.google.inject.Inject;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableHandler;
import org.jetbrains.annotations.Nullable;

final class EconomyPayableHandlerImpl implements PayableHandler {

  @Nullable
  private final EconomyProvider provider;

  @Inject
  public EconomyPayableHandlerImpl(@Nullable EconomyProvider provider) {
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
