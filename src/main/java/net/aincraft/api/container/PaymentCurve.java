package net.aincraft.api.container;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.api.JobProgression;
import net.kyori.adventure.key.Keyed;

public interface PaymentCurve extends Keyed {

  BigDecimal apply(PaymentContext context);

  interface PaymentContext {

    List<JobProgression> getProgressions();

    int getLevel();

    BigDecimal getBase();
  }
}
