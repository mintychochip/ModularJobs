package net.aincraft.api;

import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableType;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;

public interface Job extends Keyed {

  Component getDisplayName();

  Component getDescription();

  PayableCurve getCurve(PayableType type);

  void setCurve(PayableType type, PayableCurve curve);

  int getMaxLevel();

}
