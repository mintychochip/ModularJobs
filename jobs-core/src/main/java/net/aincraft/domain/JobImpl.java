package net.aincraft.domain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.math.ExpressionCurves;
import net.aincraft.registry.Registry;
import net.aincraft.util.KeyUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

record JobImpl(Key key, Component displayName, Component description, int maxLevel,
               LevelingCurve levelingCurve, Map<Key, PayableCurve> payableCurves,
               int upgradeLevel, Map<Integer, List<String>> perkUnlocks,
               Map<String, Map<Integer, List<String>>> petPerks,
               Map<String, List<String>> petRevokedPerks) implements Job {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  @Override
  public String getPlainName() {
    return PlainTextComponentSerializer.plainText().serialize(displayName);
  }

  public JobRecord toRecord() {
    return new JobRecord(
        key.toString(),
        MINI_MESSAGE.serialize(displayName),
        MINI_MESSAGE.serialize(description),
        maxLevel(),
        levelingCurve.toString(),
        serializePayableCurves(),
        upgradeLevel(),
        perkUnlocks(),
        petPerks(),
        petRevokedPerks()
    );
  }

  public static JobImpl fromRecord(
      JobRecord record,
      Plugin plugin,
      Registry<PayableType> payableTypeRegistry
  ) {
    Map<Key, PayableCurve> curves = new HashMap<>();
    for (Map.Entry<String, String> entry : record.payableCurves().entrySet()) {
      Key payableTypeKey = KeyUtils.parseKey(plugin, entry.getKey());
      if (payableTypeRegistry.isRegistered(payableTypeKey)) {
        curves.put(payableTypeKey, ExpressionCurves.payableCurve(entry.getValue()));
      }
    }

    return new JobImpl(
        KeyUtils.parseKey(plugin, record.jobKey()),
        MINI_MESSAGE.deserialize(record.displayName()),
        MINI_MESSAGE.deserialize(record.description()),
        record.maxLevel(),
        ExpressionCurves.levelingCurve(record.levellingCurve()),
        curves,
        record.upgradeLevel(),
        record.perkUnlocks(),
        record.petPerks(),
        record.petRevokedPerks()
    );
  }

  private Map<String, String> serializePayableCurves() {
    return payableCurves().entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey().toString(),
            e -> e.getValue().toString()
        ));
  }
}
