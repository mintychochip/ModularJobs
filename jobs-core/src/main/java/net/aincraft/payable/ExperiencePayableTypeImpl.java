package net.aincraft.payable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

record ExperiencePayableTypeImpl(PayableHandler handler, Key key) implements PayableType {

  private static final String FORMAT = "<#dac65c><amount>xp</#dac65c>";

  @Override
  public Component render(PayableAmount amount, int places) {
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMinimumFractionDigits(places);
    nf.setMaximumFractionDigits(places);
    BigDecimal value = amount.value().setScale(places, RoundingMode.HALF_UP);
    return MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder().tag("amount",
        Tag.inserting(Component.text(nf.format(value)))).build());
  }
}
