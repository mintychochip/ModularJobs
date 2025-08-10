package net.aincraft.api.container;

import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.service.ProgressionService;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;

public class PayableTypes {

  private PayableTypes() {
    throw new UnsupportedOperationException();
  }

  public static final PayableType EXPERIENCE = type(context -> {
    OfflinePlayer player = context.getPlayer();
    Job job = context.getJob();
    Payable payable = context.getPayable();
    ProgressionService progressionService = ProgressionService.progressionService();
    JobProgression progression = progressionService.get(player, job);
    if (progression == null) {
      progression = progressionService.create(player, job);
    }
    progression.addExperience(payable.getAmount().getAmount().longValue());
    progressionService.update(player, job, progression.getExperience());
  }, "experience");

  public static final PayableType ECONOMY = type(context -> {
    Bridge.bridge().economy().deposit(context.getPlayer(), context.getPayable().getAmount());
  }, "economy");

  private static PayableType type(PayableHandler handler, String keyString) {
    Key key = new NamespacedKey("jobs", keyString);
    PayableType type = PayableType.create(handler,key);
    RegistryContainer.registryContainer()
        .editRegistry(RegistryKeys.PAYABLE_TYPES, r -> r.register(type));
    return type;
  }

}
