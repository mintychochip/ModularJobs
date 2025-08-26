package net.aincraft;

import java.util.Map;
import java.util.Optional;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface Job extends Keyed {

  @NotNull
  Component displayName();

  String getPlainName();

  @NotNull
  Component description();

  @NotNull
  LevelingCurve levelingCurve();

  @NotNull
  Map<Key, PayableCurve> payableCurves();

  int maxLevel();
}
