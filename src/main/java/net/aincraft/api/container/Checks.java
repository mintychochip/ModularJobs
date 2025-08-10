package net.aincraft.api.container;

import java.util.function.Predicate;
import org.bukkit.GameMode;

public class Checks {

  public static final class Player {

    public static final Predicate<org.bukkit.entity.Player> NOT_PAY_WHILE_RIDING = player ->
        player.isInsideVehicle() && !ConfigurationValues.PAY_WHILE_RIDING.get();
    public static final Predicate<org.bukkit.entity.Player> NOT_PAY_IN_CREATIVE = player ->
        player.getGameMode() == GameMode.CREATIVE && !ConfigurationValues.PAY_IN_CREATIVE.get();
  }
}
