package net.aincraft.hooks;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.Store;
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
  public Optional<Boost> getBoost(BoostContext context) {
    Player player = context.player();
    if (!store.contains(player.getUniqueId())) {
      return Optional.empty();
    }
    SuperAbilityType type = store.get(player.getUniqueId());
    BigDecimal amount = boostAmounts.get(type);
    return Optional.of(Boost.multiplicative(BoostType.MCMMO, amount));
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
