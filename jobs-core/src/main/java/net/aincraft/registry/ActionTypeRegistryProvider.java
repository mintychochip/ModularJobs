package net.aincraft.registry;

import com.google.inject.Provider;
import net.aincraft.container.ActionType;
import net.kyori.adventure.key.Key;

final class ActionTypeRegistryProvider implements Provider<Registry<ActionType>> {

  @Override
  public Registry<ActionType> get() {
    SimpleRegistryImpl<ActionType> r = new SimpleRegistryImpl<>();
    r.register(() -> Key.key("jobs:block_place"));
    r.register(() -> Key.key("jobs:block_break"));
    r.register(() -> Key.key("jobs:tnt_break"));
    r.register(() -> Key.key("jobs:kill"));
    r.register(() -> Key.key("jobs:dye"));
    r.register(() -> Key.key("jobs:strip_log"));
    r.register(() -> Key.key("jobs:craft"));
    r.register(() -> Key.key("jobs:fish"));
    r.register(() -> Key.key("jobs:smelt"));
    r.register(() -> Key.key("jobs:brew"));
    r.register(() -> Key.key("jobs:enchant"));
    r.register(() -> Key.key("jobs:repair"));
    r.register(() -> Key.key("jobs:breed"));
    r.register(() -> Key.key("jobs:tame"));
    r.register(() -> Key.key("jobs:shear"));
    r.register(() -> Key.key("jobs:milk"));
    r.register(() -> Key.key("jobs:explore"));
    r.register(() -> Key.key("jobs:eat"));
    r.register(() -> Key.key("jobs:collect"));
    r.register(() -> Key.key("jobs:bake"));
    r.register(() -> Key.key("jobs:bucket"));
    r.register(() -> Key.key("jobs:brush"));
    r.register(() -> Key.key("jobs:wax"));
    r.register(() -> Key.key("jobs:villager_trade"));
    return r;
  }
}
