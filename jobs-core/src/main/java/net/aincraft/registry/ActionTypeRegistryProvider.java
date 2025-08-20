package net.aincraft.registry;

import com.google.inject.Inject;
import com.google.inject.Provider;
import net.aincraft.container.ActionType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class ActionTypeRegistryProvider implements Provider<Registry<ActionType>> {

  private final Plugin plugin;

  @Inject
  ActionTypeRegistryProvider(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public Registry<ActionType> get() {
    SimpleRegistryImpl<ActionType> r = new SimpleRegistryImpl<>();
    r.register(actionType("block_place"));
    r.register(actionType("block_break"));
    r.register(actionType("tnt_break"));
    r.register(actionType("kill"));
    r.register(actionType("dye"));
    r.register(actionType("strip_log"));
    r.register(actionType("craft"));
    r.register(actionType("fish"));
    r.register(actionType("smelt"));
    r.register(actionType("brew"));
    r.register(actionType("enchant"));
    r.register(actionType("repair"));
    r.register(actionType("breed"));
    r.register(actionType("tame"));
    r.register(actionType("shear"));
    r.register(actionType("milk"));
    r.register(actionType("explore"));
    r.register(actionType("eat"));
    r.register(actionType("collect"));
    r.register(actionType("bake"));
    r.register(actionType("bucket"));
    r.register(actionType("brush"));
    r.register(actionType("wax"));
    r.register(actionType("villager_trade"));
    return r;
  }

  private ActionType actionType(String keyString) {
    return () -> new NamespacedKey(plugin, keyString);
  }
}
