package net.aincraft.api.container;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
public interface BoostType extends Keyed {


  interface Typed<T> extends BoostType {

  }

  BoostType.Typed<Plugin> MCMMO = type(Key.key("jobs:player"));

  BoostType.Typed<Player> PLAYER = type(Key.key("jobs:player"));

  BoostType.Typed<Plugin> PLUGIN = type(Key.key(""));

  private static <T> BoostType.Typed<T> type(Key key) {
    return new Typed<T>() {
      @Override
      public @NotNull Key key() {
        return key;
      }
    };
  }
}
