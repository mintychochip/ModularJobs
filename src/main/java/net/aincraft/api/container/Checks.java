package net.aincraft.api.container;

import java.util.Locale;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Checks {

  public static final class Player {

    public static final Predicate<org.bukkit.entity.Player> NOT_PAY_WHILE_RIDING = player ->
        player.isInsideVehicle() && !ConfigurationValues.PAY_WHILE_RIDING.get();

    public static final Predicate<org.bukkit.entity.Player> NOT_PAY_IN_CREATIVE = player ->
        player.getGameMode() == GameMode.CREATIVE && !ConfigurationValues.PAY_IN_CREATIVE.get();

    public static final Predicate<org.bukkit.entity.Player> NOT_ONLINE = player -> !player.isOnline();
  }

  public static final class World {

    public static final Predicate<org.bukkit.World> WORLD_DISABLED = world -> ConfigurationValues.DISABLED_WORLDS.get()
        .contains(world.getName().toLowerCase(
            Locale.ENGLISH));
  }
}
