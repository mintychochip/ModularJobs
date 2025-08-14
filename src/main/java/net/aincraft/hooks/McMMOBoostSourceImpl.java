package net.aincraft.hooks;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
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

public class McMMOBoostSourceImpl implements McMMOBoostSource {

  private final Store<UUID, SuperAbilityType> store;

  private Provider<SuperAbilityType, Double> boostAmountProvider;

  public McMMOBoostSourceImpl(Store<UUID, SuperAbilityType> store,
      Provider<SuperAbilityType, Double> boostAmountProvider) {
    this.store = store;
    this.boostAmountProvider = boostAmountProvider;
  }

  public static McMMOBoostSourceImpl create(Plugin plugin, Registry<BoostSource> sources) {
    Store<UUID, SuperAbilityType> store = Store.memory();
    Bukkit.getPluginManager().registerEvents(new McMMOController(store), plugin);
    Provider<SuperAbilityType, Double> provider = new Provider<>() {
      @Override
      public @NotNull Optional<Double> get(SuperAbilityType key) {
        return Optional.ofNullable(amounts.get(key));
      }

      private static final Map<SuperAbilityType, Double> amounts = new HashMap<>();

      static {
        amounts.put(SuperAbilityType.TREE_FELLER, 1.0);
      }

    };
    McMMOBoostSourceImpl source = new McMMOBoostSourceImpl(store, provider);
    sources.register(source);
    return source;
  }

  @Override
  public Optional<Boost> getBoost(BoostContext context) {
    Player player = context.getPlayer();
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

  @Override
  public void setBoostAmountProvider(Provider<SuperAbilityType, Double> boostAmountProvider) {
    this.boostAmountProvider = boostAmountProvider;
  }
}
