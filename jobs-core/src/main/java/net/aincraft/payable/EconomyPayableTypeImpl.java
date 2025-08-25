package net.aincraft.payable;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import net.aincraft.container.Currency;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

record EconomyPayableTypeImpl(PayableHandler handler, Key key) implements PayableType {

  private static final String FORMAT = "<#7ed278><symbol><amount></#7ed278>";

  @Inject
  public EconomyPayableTypeImpl {
  }

  @Override
  public Component render(PayableAmount amount, int places) {
    //TODO: explicitly code a currency USD for the else
    String symbol = amount.currency().map(Currency::symbol).orElse("$");
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMinimumFractionDigits(places);
    nf.setMaximumFractionDigits(places);
    BigDecimal value = amount.value().setScale(places, RoundingMode.HALF_UP);
    return MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder()
        .tag("symbol", Tag.inserting(Component.text(symbol)))
        .tag("amount", Tag.inserting(Component.text(nf.format(value))))
        .build());
  }
}
