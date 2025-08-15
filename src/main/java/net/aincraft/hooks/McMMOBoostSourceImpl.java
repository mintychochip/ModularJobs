package net.aincraft.hooks;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.Provider;
import net.aincraft.api.container.Store;
import net.aincraft.api.registry.Registry;
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

  private final Provider<SuperAbilityType, BigDecimal> boostAmountProvider;

  public McMMOBoostSourceImpl(Store<UUID, SuperAbilityType> store,
      Provider<SuperAbilityType, BigDecimal> boostAmountProvider) {
    this.store = store;
    this.boostAmountProvider = boostAmountProvider;
  }

  public static McMMOBoostSourceImpl create(Plugin plugin,
      Provider<SuperAbilityType, BigDecimal> boostAmountProvider) {
    Store<UUID, SuperAbilityType> store = Store.memory();
    Bukkit.getPluginManager().registerEvents(new McMMOController(store), plugin);
    return new McMMOBoostSourceImpl(store, boostAmountProvider);
  }

  @Override
  public Optional<Boost> getBoost(BoostContext context) {
    Player player = context.player();
    if (!store.contains(player.getUniqueId())) {
      return Optional.empty();
    }
    SuperAbilityType type = store.get(player.getUniqueId());
    return boostAmountProvider.get(type)
        .map(amount -> Boost.multiplicative(BoostType.MCMMO, amount));
  }

  @Override
  public @NotNull Key key() {
    return Key.key("jobs:mcmmo");
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
}
