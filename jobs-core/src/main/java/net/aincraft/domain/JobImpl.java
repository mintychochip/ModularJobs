package net.aincraft.domain;

import java.util.Map;
import net.aincraft.Job;
import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

record JobImpl(Key key, Component displayName, Component description, int maxLevel,
               LevelingCurve levelingCurve, Map<Key, PayableCurve> payableCurves) implements Job {

  @Override
  public String getPlainName() {
    return PlainTextComponentSerializer.plainText().serialize(displayName);
  }
}
