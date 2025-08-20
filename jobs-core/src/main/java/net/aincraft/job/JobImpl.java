package net.aincraft.job;

import java.util.Map;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class JobImpl implements Job {

  private final Key key;
  private final Component displayName;
  private final Component description;
  private final LevelingCurve levelingCurve;
  private final Map<PayableType, PayableCurve> payableCurves;

  public JobImpl(Key key, Component displayName,  Component description,
      LevelingCurve levelingCurve,
      Map<PayableType, PayableCurve> payableCurves) {
    this.key = key;
    this.displayName = displayName;
    this.description = description;
    this.levelingCurve = levelingCurve;
    this.payableCurves = payableCurves;
  }

  @Override
  public Component getDisplayName() {
    return displayName;
  }

  @Override
  public String getPlainName() {
    return PlainTextComponentSerializer.plainText().serialize(displayName);
  }

  @Override
  public Component getDescription() {
    return description;
  }

  @Override
  public LevelingCurve getLevelingCurve() {
    return levelingCurve;
  }

  @Override
  public Optional<PayableCurve> getCurve(PayableType type) {
    return Optional.ofNullable(payableCurves.get(type));
  }

  @Override
  public int getMaxLevel() {
    return 200;
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}
