package net.aincraft.hooks;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.container.Store;
import net.aincraft.container.*;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class McMMOBoostSourceImpl implements BoostSource {

  private final Store<UUID, SuperAbilityType> store;

  private final Map<SuperAbilityType, BigDecimal> boostAmounts;

  public McMMOBoostSourceImpl(Store<UUID, SuperAbilityType> store,
      Map<SuperAbilityType, BigDecimal> boostAmounts) {
    this.store = store;
    this.boostAmounts = boostAmounts;
  }

  public static McMMOBoostSourceImpl create(Plugin plugin,
      Map<SuperAbilityType, BigDecimal> boostAmounts) {
    Store<UUID, SuperAbilityType> store = Store.memory();
    Bukkit.getPluginManager().registerEvents(new McMMOController(store), plugin);
    return new McMMOBoostSourceImpl(store, boostAmounts);
  }

  @Override
  public @NotNull List<Boost> evaluate(BoostContext context) {
    Player player = context.player();
    if (!store.contains(player.getUniqueId())) {
      return List.of();
    }
    SuperAbilityType type = store.get(player.getUniqueId());
    BigDecimal amount = boostAmounts.get(type);
    return List.of(Boost.multiplicative(BoostType.MCMMO, amount));
  }

  private record McMMOController(Store<UUID, SuperAbilityType> store) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onAbilityOn(final McMMOPlayerAbilityActivateEvent event) {
      Player player = event.getPlayer();
      store.add(player.getUniqueId(), event.getAbility());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onAbilityOff(final McMMOPlayerAbilityDeactivateEvent event) {
      store.remove(event.getPlayer().getUniqueId());
    }
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:mcmmo_source");
  }
}
